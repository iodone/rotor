package rotor.engine.common

import java.io.{BufferedReader, File, InputStreamReader}

import org.apache.commons.io.FileUtils
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs._

import scala.collection.mutable.ArrayBuffer


object HDFSOperator {

  def readFile(path: String): String = {
    val fs = FileSystem.get(new Configuration())
    var br: BufferedReader = null
    var line: String = null
    val result = new ArrayBuffer[String]()
    try {
      br = new BufferedReader(new InputStreamReader(fs.open(new Path(path))))
      line = br.readLine()
      while (line != null) {
        result += line
        line = br.readLine()
      }
    } finally {
      if (br != null) br.close()
    }
    result.mkString("\n")
  }

  def readBinaryFile(path: String, length: Int): Array[Byte] = {
    val fs = FileSystem.get(new Configuration())

    val buf = new Array[Byte](length)
    val result = new ArrayBuffer[Byte]()
    var fp: FSDataInputStream = null

    try {
      fp = fs.open(new Path(path))
      var n = 0
      while (n >= 0) {
        n = fp.read(buf, 0, length)
        result ++= buf.slice(0, n)
      }
    } finally {
      if (fp != null) fp.close()
    }
    result.toArray
  }

  def listModelDirectory(path: String): Seq[FileStatus] = {
    val fs = FileSystem.get(new Configuration())
    fs.listStatus(new Path(path)).filter(f => f.isDirectory)
  }

  def saveBytesFile(path: String, fileName: String, bytes: Array[Byte]) = {

    var dos: FSDataOutputStream = null
    try {

      val fs = FileSystem.get(new Configuration())
      if (!fs.exists(new Path(path))) {
        fs.mkdirs(new Path(path))
      }
      dos = fs.create(new Path(new java.io.File(path, fileName).getPath), true)
      dos.write(bytes)
    } catch {
      case ex: Exception =>
        println("file save exception")
    } finally {
      if (null != dos) {
        try {
          dos.close()
        } catch {
          case ex: Exception =>
            println("close exception")
        }
        dos.close()
      }
    }

  }


  def saveFile(path: String, fileName: String, iterator: Iterator[(String, String)]) = {

    var dos: FSDataOutputStream = null
    try {

      val fs = FileSystem.get(new Configuration())
      if (!fs.exists(new Path(path))) {
        fs.mkdirs(new Path(path))
      }
      dos = fs.create(new Path(path + s"/$fileName"), true)
      iterator.foreach { x =>
        dos.writeBytes(x._2 + "\n")
      }
    } catch {
      case ex: Exception =>
        println("file save exception")
    } finally {
      if (null != dos) {
        try {
          dos.close()
        } catch {
          case ex: Exception =>
            println("close exception")
        }
        dos.close()
      }
    }

  }

  def copyToHDFS(tempLocalPath: String, path: String, cleanTarget: Boolean, cleanSource: Boolean) = {
    val fs = FileSystem.get(new Configuration())
    // fs.delete(new Path(path), true)
    if (cleanTarget) {
      fs.delete(new Path(path), true)
    }
    fs.copyFromLocalFile(new Path(tempLocalPath),
      new Path(path))
    // FileUtils.forceDelete(new File(tempLocalPath))
    if (cleanSource) {
      FileUtils.forceDelete(new File(tempLocalPath))
    }
  }

  def copyToLocalFile(tempLocalPath: String, path: String, clean: Boolean) = {
    val fs = FileSystem.get(new Configuration())
    val tmpFile = new File(tempLocalPath)
    if (tmpFile.exists()) {
      FileUtils.forceDelete(tmpFile)
    }
    fs.copyToLocalFile(new Path(path), new Path(tempLocalPath))
  }

  def deleteDir(path: String) = {
    val fs = FileSystem.get(new Configuration())
    fs.delete(new Path(path), true)
  }

  def createDir(path: String) = {
    val fs = FileSystem.get(new Configuration())
    fs.mkdirs(new Path(path))
  }

  def createTempModelLocalPath(path: String, autoCreateParentDir: Boolean = true) = {
    val dir = "/tmp/train/" + Md5.md5Hash(path)
    if (autoCreateParentDir) {
      FileUtils.forceMkdir(new File(dir))
    }
    dir
  }

  def isDir(path: String) = {
    val fs = FileSystem.get(new Configuration())
    fs.isDirectory(new Path(path))
  }

  def fileExists(path: String) = {
    val fs = FileSystem.get(new Configuration())
    fs.exists(new Path(path))
  }

  def isFile(path: String) = {
    val fs = FileSystem.get(new Configuration())
    fs.isFile(new Path(path))
  }


  def main(args: Array[String]): Unit = {
//    println(readFile("file:///flink.json"))

    val path = "file:///home/work/data/dataset/apt/test0.jpg"
    val file = readBinaryFile(path, 1024)
    println(file.length)
//    val fileStr =  new String(file, StandardCharsets.UTF_8)
//    println(fileStr)
  }
}
