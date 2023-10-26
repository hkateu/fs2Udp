package com.rockthejvm.interfaces

import cats.effect.{IOApp, ExitCode, IO}
import java.net.NetworkInterface
import java.util.Enumeration
import java.net.InetAddress

object GetInterfaces extends IOApp:
  def getInterfaces = {
      val enumInterfaces: Enumeration[NetworkInterface] = NetworkInterface.getNetworkInterfaces()
      while (enumInterfaces.hasMoreElements()) {
        val ni: NetworkInterface = enumInterfaces.nextElement()
        val enumIP: Enumeration[InetAddress] = ni.getInetAddresses()
        println(s"""
          Network Interface: ${ni.getDisplayName()}
          - Up and running: ${ni.isUp()}
          - Supports Multicasting: ${ni.supportsMulticast()}
          - Name: ${ni.getName()}
          - Is virtual: ${ni.isVirtual()}
          - Ip Addresses:
        """)
        while (enumIP.hasMoreElements()) {
          val ip: InetAddress = enumIP.nextElement()
          println(s"""
               -$ip 
          """)
        }
      }
    }
  def run(args: List[String]): IO[ExitCode] = IO(getInterfaces).as(ExitCode.Success)