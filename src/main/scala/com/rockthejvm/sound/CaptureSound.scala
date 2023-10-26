package com.rockthejvm.sound

import cats.effect.{IO, IOApp, ExitCode}
import javax.sound.sampled.AudioFormat
import java.io.OutputStream
import javax.sound.sampled.DataLine
import javax.sound.sampled.DataLine.Info
import javax.sound.sampled.TargetDataLine
import javax.sound.sampled.AudioSystem
import com.comcast.ip4s.*
import fs2.{io, Stream}
import fs2.io.net.{Network, SocketOption, Datagram}
import java.net.StandardSocketOptions
import java.net.NetworkInterface
import java.net.StandardProtocolFamily

object CaptureSound extends IOApp {
  private def getAudioFormat: AudioFormat = {
    val sampleRate = 44100.0f
    val sampleSizeInBits = 16
    val channels = 2
    val signed = true
    val bigEdian = true

    new AudioFormat(
      sampleRate,
      sampleSizeInBits,
      channels,
      signed,
      bigEdian
    )
  }

  private def capture(out: OutputStream): Unit = {
    val info: Info = new DataLine.Info(classOf[TargetDataLine], getAudioFormat)
    val line: TargetDataLine =
      AudioSystem.getLine(info).asInstanceOf[TargetDataLine]
    val data: Array[Byte] = new Array[Byte](1024)

    if (AudioSystem.isLineSupported(info)) {
      line.open()
      line.start()
      println("Start capturing...")
      while (line.isOpen()) {
        val numBytesRead: Int = line.read(data, 0, data.length)
        out.write(data, 0, numBytesRead)
      }
    } else {
      println("line not supported")
    }
  }

  def server = {
    val address = SocketAddress(ip"225.4.5.6", port"5555")
    Stream
      .resource(
        Network[IO].openDatagramSocket(
          port = Some(port"5555"),
          options = List(
            SocketOption(
              StandardSocketOptions.IP_MULTICAST_IF,
              NetworkInterface.getByName("wlxb4b024bc35a7")
            ),
            SocketOption(
              StandardSocketOptions.SO_REUSEADDR,
              true
            )
          ),
          protocolFamily = Some(StandardProtocolFamily.INET)
        )
      )
      .flatMap { socket =>
        io.readOutputStream(1024)(out => IO(capture(out)))
          .chunks
          .map(data => Datagram(address, data))
          .through(socket.writes)
          .drain
      }
      .handleErrorWith { error =>
        Stream.eval(
          IO.println(s"[multicast client] Error: ${error.getMessage}")
        )
      }
  }

  def run(args: List[String]): IO[ExitCode] =
    server.compile.drain.as(ExitCode.Success)
}
