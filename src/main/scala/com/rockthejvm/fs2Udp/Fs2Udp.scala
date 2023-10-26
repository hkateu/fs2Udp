package com.rockthejvm.fs2Udp

import cats.effect.{IOApp, ExitCode, IO}
import fs2.Stream
import fs2.io.net.Network
import com.comcast.ip4s.*
import fs2.io.net.SocketOption
import java.net.StandardSocketOptions
import java.net.StandardProtocolFamily
import fs2.io.net.DatagramSocket
import fs2.text
import fs2.io.net.Datagram
import scala.concurrent.duration.*

object Fs2Udp extends IOApp {
  def server =
    Stream
      .resource(
        Network[IO].openDatagramSocket(
          address = Some(ip"127.0.0.1"),
          port = Some(port"5555"),
          options = List(
            SocketOption(StandardSocketOptions.SO_RCVBUF, 1024 * 4),
            SocketOption(StandardSocketOptions.SO_SNDBUF, 1024 * 4)
          ),
          protocolFamily = Some(StandardProtocolFamily.INET)
        )
      )
      .evalTap(socket =>
        IO.println("[server] Udp server is successfully opened") *>
          socket.localAddress.flatMap { address =>
            IO.println(s"[server] Udp server is bound to: ${address.toString}")
          }
      )
      .flatMap { socket =>
        socket.reads
          .evalTap { datagram =>
            IO.println(
              s"[server] I've received ${datagram.bytes.size} bytes " +
                s"from ${datagram.remote.toString}! Sending them back ..."
            )
          }
          .through(socket.writes)
      }
      .handleErrorWith { error =>
        Stream.eval(
          IO.println(s"[server] ${error.getMessage}")
        )
      }

  def client = {
    val serverAddress: SocketAddress[IpAddress] =
      SocketAddress(ip"127.0.0.1", port"5555")
    Stream
      .resource(Network[IO].openDatagramSocket())
      .flatMap { socket =>
        Stream("[message] I was sent back from the server!")
          .through(text.utf8.encode)
          .chunks
          .flatMap { data =>
            Stream(Datagram(serverAddress, data))
              .evalTap { datagram =>
                IO.println(
                  s"[client] Sending ${datagram.bytes.size} bytes to the Echo Server!"
                )
              }
          }
          .through(socket.writes)
          .drain ++
          socket.reads
            .flatMap(datagram => Stream.chunk(datagram.bytes))
            .through(text.utf8.decode)
            .foreach { response =>
              IO.println(response)
            }
      }
      .handleErrorWith { error =>
        Stream.eval(
          IO.println(s"[client] ${error.getMessage}")
        )
      }
  }

  def run(args: List[String]): IO[ExitCode] =
    server
      .concurrently(Stream.sleep[IO](500.millis) ++ client)
      .compile
      .drain
      .as(ExitCode.Success)
}
