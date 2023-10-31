package com.rockthejvm.sound

import javax.sound.sampled.AudioFormat
import java.io.OutputStream
import javax.sound.sampled.Line
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.AudioFileFormat
import java.io.File
import javax.sound.sampled.DataLine.Info
import javax.sound.sampled.AudioInputStream
import cats.effect.*
import scala.concurrent.duration.*
import cats.syntax.parallel.*

object RecordSound extends IOApp:
  val fileType = AudioFileFormat.Type.WAVE
  val wavFile = new File(
    "src/main/scala/com/rockthejvm/sound/Record.wav"
  )

  def getAudioFormat: AudioFormat = {
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

  val info: Info = new DataLine.Info(classOf[TargetDataLine], getAudioFormat)
  val line: TargetDataLine =
    AudioSystem.getLine(info).asInstanceOf[TargetDataLine]
  def start: Unit = {

    val data: Array[Byte] = new Array[Byte](1024)

    if (AudioSystem.isLineSupported(info)) {
      line.open()
      line.start()
      println("Start capturing...")
      while (line.isOpen()) {
        val ais = new AudioInputStream(line)
        val numBytesRead = ais.read(data, 0, 1024)
        AudioSystem.write(ais, fileType, wavFile)
      }
    } else {
      println("line not supported")
    }
  }

  def stop = {
    line.stop()
    line.close()
    println("Finished")
  }

  def run(args: List[String]): IO[ExitCode] =
    (IO(start), IO.sleep(10000.millis) *> IO(stop))
      .parMapN((st, sp) => ())
      .as(ExitCode.Success)
