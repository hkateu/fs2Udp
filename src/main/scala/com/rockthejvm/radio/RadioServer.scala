package com.rockthejvm.radio

import cats.effect.{IOApp, ExitCode, IO}
import com.comcast.ip4s.*
import fs2.io
import java.net.URL
import fs2.Stream
import fs2.io.net.{Network, SocketOption, Datagram}
import java.net.{
  StandardSocketOptions,
  StandardProtocolFamily,
  NetworkInterface
}

object RadioServer extends IOApp {
  def radioServer(link: String) = {
    val multicastAddress: SocketAddress[IpAddress] =
      SocketAddress(ip"225.4.5.6", port"5555")
    val url: Stream[IO, Byte] = io.readInputStream[IO](
      IO(
        new URL(
          link
        ).openConnection.getInputStream
      ),
      1024
    )
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
            "[multicast server] The Radio stream is starting..."
          )
      }
      .flatMap { socket =>
        url.chunks
          .map(data => Datagram(multicastAddress, data))
          .through(socket.writes)
          .drain
      }
      .handleErrorWith { error =>
        Stream.eval(
          IO.println(s"[multicast server] Error: ${error.getMessage}")
        )
      }
  }

  override def run(args: List[String]): IO[ExitCode] =
    radioServer(
      "http://media-ice.musicradio.com:80/ClassicFM-M-Movies"
    ).compile.drain
      .as(ExitCode.Success)
}
