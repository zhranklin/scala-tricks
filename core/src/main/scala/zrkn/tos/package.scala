package zrkn

import com.volcengine.tos.{TOSClientConfiguration, TOSV2ClientBuilder}
import com.volcengine.tos.auth.StaticCredentials
import com.volcengine.tos.comm.common.ACLType
import com.volcengine.tos.model.`object`.{DeleteObjectInput, ListObjectsType2Input, ObjectMetaRequestOptions, PutObjectInput}

import java.io.ByteArrayInputStream
import java.util.UUID
import scala.jdk.CollectionConverters._

package object tos:
  var AK = ""
  var SK = ""
  var REGION = "cn-beijing"
  var ENDPOINT = "tos-cn-beijing.volces.com"
  var BUCKET = ""


  lazy private val tos = new TOSV2ClientBuilder().build:
    TOSClientConfiguration.builder()
      .region(REGION)
      .endpoint(ENDPOINT)
      .credentials(new StaticCredentials(AK, SK))
      .build()

  // 如果要上传本地文件，推荐使用 uploadTOS(os.read.bytes(file))
  // 如果上传url指定的文件，推荐使用 uploadTOS(requests.get(url).bytes)
  def uploadTOS(file: Array[Byte], postfix: String = ""): String =
    val fn = UUID.randomUUID.toString + postfix
    val put = new PutObjectInput()
      .setBucket(BUCKET)
      .setKey(s"tmp/$fn")
      .setOptions {
        val o = new ObjectMetaRequestOptions
        o.setAclType(ACLType.ACL_PUBLIC_READ)
      }
      .setContent(new ByteArrayInputStream(file))
    val resp = tos.putObject(put)
    s"https://$BUCKET.$ENDPOINT/tmp/$fn"
  def clear(): Unit =
    val input = new ListObjectsType2Input().setBucket(BUCKET).setPrefix("tmp/")
    Iterator.continually(tos.listObjectsType2(input))
      .takeWhile(l => l.getContents != null && l.getContents.asScala.nonEmpty)
      .flatMap(l => l.getContents.asScala)
      .map(o => new DeleteObjectInput().setBucket(BUCKET).setKey(o.getKey))
      .foreach: o =>
        tos.deleteObject(o)
        println("Deleted: " + o.getKey)

