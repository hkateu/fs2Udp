package com.rockthejvm.fs2Udp

import cats.effect.{IOApp, ExitCode, IO}
import com.comcast.ip4s.*
import fs2.{Stream, text}
import fs2.io.net.Network
import fs2.io.net.SocketOption
import java.net.StandardSocketOptions
import java.net.StandardProtocolFamily
import java.net.NetworkInterface
import java.util.Date
import fs2.io.net.Datagram
import scala.concurrent.duration.*

object Fs2UdpMulticasting extends IOApp {
  def server = {
    val multicastAddress = SocketAddress(ip"225.4.5.6", port"5555")
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
      .evalTap { socket =>
        IO.println("[multicast server] Udp server is successfully opened") *>
          socket.localAddress.flatMap(addr =>
            IO.println(
              s"[multicast server] Udp server is bound to: ${addr.toString}"
            )
          ) *>
          IO.println(
            "[multicast server] Udp server will start sending date time info shortly..."
          )
      }
      .flatMap { socket =>
        Stream
          .repeatEval(IO(new Date().toString()))
          .through(text.utf8.encode)
          .chunks
          .map(data => Datagram(multicastAddress, data))
          .metered(10000.millis)
          .evalTap(_ => IO.println("Sending data ..."))
          .through(socket.writes)
          .drain
      }
      .handleErrorWith { error =>
        Stream.eval(
          IO.println(s"[multicast server] Error: ${error.getMessage}")
        )
      }
  }

  def client = {
    Stream
      .resource(
        Network[IO].openDatagramSocket(
          port = Some(port"5555"),
          options = List(
            SocketOption(
              StandardSocketOptions.SO_REUSEADDR,
              true
            )
          ),
          protocolFamily = Some(StandardProtocolFamily.INET)
        )
      )
      .evalTap { socket =>
        socket.localAddress.flatMap(addr =>
          IO.println(
            s"[multicast server] client is bound to: ${addr.toString}"
          )
        )
      }
      .flatMap { socket =>
        val groupMembership: IO[socket.GroupMembership] = socket.join(
          MulticastJoin.fromString("225.4.5.6").get,
          NetworkInterface.getByName("wlxb4b024bc35a7")
        )

        Stream.eval(groupMembership) ++
          socket.reads
            .flatMap(datagram => Stream.chunk(datagram.bytes))
            .through(text.utf8.decode)
            .foreach { response =>
              IO.println(response)
            }
      }
      .handleErrorWith { error =>
        Stream.eval(
          IO.println(s"[multicast client] Error: ${error.getMessage}")
        )
      }
  }

  def run(args: List[String]): IO[ExitCode] =
    server.concurrently(client).compile.drain.as(ExitCode.Success)
}
