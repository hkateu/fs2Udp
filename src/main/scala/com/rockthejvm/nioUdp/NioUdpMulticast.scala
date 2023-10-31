package com.rockthejvm.nioUdp

import cats.effect.{IOApp, ExitCode, IO}
import java.nio.ByteBuffer
import scala.util.{Try, Failure, Success}
import java.nio.channels.DatagramChannel
import java.net.{
  StandardProtocolFamily,
  StandardSocketOptions,
  InetSocketAddress,
  NetworkInterface,
  InetAddress
}
import java.util.Date
import java.nio.charset.Charset
import java.nio.charset.CharsetDecoder
import java.nio.channels.MembershipKey
import cats.syntax.parallel.*

object NioUdpMulticast extends IOApp {
  def server: Unit = {
    val port: Int = 5555
    val groupIp: String = "225.4.5.6"
    val content: ByteBuffer = ByteBuffer.allocate(65507)

    Try {
      val datagramChannel: DatagramChannel =
        DatagramChannel.open(StandardProtocolFamily.INET)

      if (datagramChannel.isOpen()) {
        println("[multicast server] Udp server is successfully opened")

        val networkInterface: NetworkInterface =
          NetworkInterface.getByName(
            "wlxb4b024bc35a7"
          ) // Pass the interface name that matches what you have.

        datagramChannel.setOption(
          StandardSocketOptions.IP_MULTICAST_IF,
          networkInterface
        )
        datagramChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true)

        datagramChannel.bind(new InetSocketAddress(port))
        println(
          s"[multicast server] Udp server is bound to: ${datagramChannel.getLocalAddress()}"
        )

        println(
          "[multicast server] Udp server will start sending date time info shortly..."
        )

        while (true) {
          try {
            Thread.sleep(10000)
          } catch {
            case e => println(e.getMessage)
          }

          println("Sending data ...")
          val datetime = ByteBuffer.wrap(new Date().toString().getBytes())
          datagramChannel.send(
            datetime,
            new InetSocketAddress(InetAddress.getByName(groupIp), port)
          )
          datetime.flip()
        }
      } else {
        println("[multicast server] The channel cannot be opened!")
      }
    } match {
      case Failure(ex) =>
        println(s"[multicast server] ${ex.getMessage}")
      case Success(_) =>
        println("[multicast server] Everything works fine")
    }
  }

  def client = {
    val port: Int = 5555
    val groupIp: String = "225.4.5.6"

    val charSet: Charset = Charset.defaultCharset()
    val decoder: CharsetDecoder = charSet.newDecoder()
    val content: ByteBuffer = ByteBuffer.allocate(65507)

    Try {
      val datagramChannel = DatagramChannel.open(StandardProtocolFamily.INET)

      val group: InetAddress = InetAddress.getByName(groupIp)
      if (group.isMulticastAddress()) {
        if (datagramChannel.isOpen()) {
          val networkInterface = NetworkInterface.getByName("wlxb4b024bc35a7")
          datagramChannel.setOption(
            StandardSocketOptions.SO_REUSEADDR,
            true
          )

          datagramChannel.bind(new InetSocketAddress(port))
          println(
            s"[multicast client] client is bound to: ${datagramChannel.getLocalAddress()}"
          )

          val key: MembershipKey = datagramChannel.join(group, networkInterface)
          while (true) {
            if (key.isValid()) {
              datagramChannel.receive(content)
              content.flip()
              val charBuffer = decoder.decode(content)
              println(charBuffer.toString())
              content.clear()
            } else {
              println("[multicast client] Invalid join key")
            }
          }
        } else {
          println("[multicast client] The channel cannot be opened!")
        }
      } else {
        println("[multicast client] This is not multicast address")
      }
    } match {
      case Failure(ex) =>
        println(s"[multicast client] ${ex.getMessage}")
      case Success(_) =>
        println("[multicast client] Everything works fine")
    }
  }
  def run(args: List[String]): IO[ExitCode] =
    (IO(server), IO(client)).parMapN((s, c) => ()).as(ExitCode.Success)
}
