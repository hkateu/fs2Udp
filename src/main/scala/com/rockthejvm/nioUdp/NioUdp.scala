package com.rockthejvm.nioUdp

import cats.effect.{IOApp, ExitCode, IO}
import scala.util.Try
import scala.util.Failure
import scala.util.Success
import java.nio.channels.DatagramChannel
import java.net.StandardProtocolFamily
import java.net.StandardSocketOptions
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.net.SocketAddress
import java.nio.charset.Charset
import java.nio.charset.CharsetDecoder
import cats.syntax.parallel.*
import scala.concurrent.duration.*
import java.net.ProtocolFamily

object NioUdp extends IOApp {
  def server: Unit = {
    val port: Int = 5555
    val ip: String = "127.0.0.1"
    val content: ByteBuffer = ByteBuffer.allocate(65507)

    Try {
      val datagramChannel: DatagramChannel =
        DatagramChannel.open(StandardProtocolFamily.INET)

      if (datagramChannel.isOpen()) {
        println("[server] Udp server is successfully opened")

        datagramChannel.setOption(StandardSocketOptions.SO_RCVBUF, 4 * 1024)
        datagramChannel.setOption(StandardSocketOptions.SO_SNDBUF, 4 * 1024)

        datagramChannel.bind(new InetSocketAddress(ip, port))
        println(
          "[server] Udp server is bound to:" + datagramChannel.getLocalAddress()
        )

        while (true) {
          val clientAddress: SocketAddress = datagramChannel.receive(content)
          content.flip()
          println(
            s"[server] I've received ${content.limit()} bytes " +
              s"from ${clientAddress.toString()}! Sending them back ..."
          )
          datagramChannel.send(content, clientAddress)
          content.clear()
        }
      } else {
        println("[server] The channel cannot be opened!")
      }
    } match {
      case Failure(ex) =>
        println(s"[server] ${ex.getMessage}")
      case Success(_) =>
        println("[server] Everything works fine")
    }
  }

  def client = {
    val serverPort: Int = 5555
    val serverIp: String = "127.0.0.1"

    val charSet: Charset = Charset.defaultCharset()
    val decoder: CharsetDecoder = charSet.newDecoder()

    val echoText: ByteBuffer =
      ByteBuffer.wrap("[message] I was sent back from the server!".getBytes())
    val content: ByteBuffer = ByteBuffer.allocate(65507)

    Try {
      val datagramChannel = DatagramChannel.open(StandardProtocolFamily.INET)
      if (datagramChannel.isOpen()) {
        datagramChannel.setOption(StandardSocketOptions.SO_RCVBUF, 4 * 1024)
        datagramChannel.setOption(StandardSocketOptions.SO_SNDBUF, 4 * 1024)

        val sent = datagramChannel.send(
          echoText,
          new InetSocketAddress(serverIp, serverPort)
        )
        println(
          s"[client] I have successfully sent $sent bytes to the Echo Server!"
        )

        datagramChannel.receive(content)

        content.flip()
        val charBuffer = decoder.decode(content)
        println(charBuffer.toString())
        content.clear()
        datagramChannel.close()
      } else {
        println("[client] The channel cannot be opened!")
      }
    } match {
      case Failure(ex) =>
        println(s"[client] ${ex.getMessage}")
      case Success(_) =>
        println("[client] Everything works fine")
    }
  }

  def connectedClient = {
    val serverPort: Int = 5555
    val serverIp: String = "127.0.0.1"

    val charSet: Charset = Charset.defaultCharset()
    val decoder: CharsetDecoder = charSet.newDecoder()

    val echoText: ByteBuffer =
      ByteBuffer.wrap("[message] I was sent back from the server!".getBytes())
    val content: ByteBuffer = ByteBuffer.allocate(65507)

    Try {
      val datagramChannel = DatagramChannel.open(StandardProtocolFamily.INET)
      if (datagramChannel.isOpen()) {
        datagramChannel.setOption(StandardSocketOptions.SO_RCVBUF, 4 * 1024)
        datagramChannel.setOption(StandardSocketOptions.SO_SNDBUF, 4 * 1024)

        datagramChannel.connect(new InetSocketAddress(serverIp, serverPort))

        if (datagramChannel.isConnected()) {
          val sent = datagramChannel.write(echoText)
          println(
            s"[client] I have successfully sent $sent bytes to the Echo Server!"
          )

          datagramChannel.read(content)

          content.flip()
          val charBuffer = decoder.decode(content)
          println(charBuffer.toString())
          content.clear()
          datagramChannel.close()
        }
      } else {
        println("[client] The channel cannot be opened!")
      }
    } match {
      case Failure(ex) =>
        println(s"[client] ${ex.getMessage}")
      case Success(_) =>
        println("[client] Everything works fine")
    }
  }

  def run(args: List[String]): IO[ExitCode] =
    (IO(server), IO.sleep(500.millis).flatMap(_ => IO(connectedClient)))
      .parMapN((s, c) => ())
      .as(ExitCode.Success)
}
