package zrkn


import java.util.UUID
import scala.jdk.CollectionConverters.*

package oss_inner_ve {
  import com.volcengine.tos.auth.StaticCredentials
  import com.volcengine.tos.comm.common.ACLType
  import com.volcengine.tos.model.`object`.{DeleteObjectInput, ListObjectsType2Input, ObjectMetaRequestOptions, PutObjectInput}
  import com.volcengine.tos.{TOSClientConfiguration, TOSV2ClientBuilder}
  import zrkn.oss.*

  import java.io.ByteArrayInputStream

  lazy private val ve = new TOSV2ClientBuilder().build:
    TOSClientConfiguration.builder()
      .region(VE_REGION)
      .endpoint(VE_ENDPOINT)
      .credentials(new StaticCredentials(VE_AK, VE_SK))
      .build()
  def upload(file: Array[Byte], postfix: String = ""): String =
    val key = "tmp/" + UUID.randomUUID.toString + postfix
    val put = new PutObjectInput()
      .setBucket(VE_BUCKET)
      .setKey(key)
      .setOptions:
        val o = new ObjectMetaRequestOptions
        o.setAclType(ACLType.ACL_PUBLIC_READ)
      .setContent(new ByteArrayInputStream(file))
    val resp = ve.putObject(put)
    s"https://$VE_BUCKET.$VE_ENDPOINT/$key"
  def clear(): Unit =
    val input = new ListObjectsType2Input().setBucket(VE_BUCKET).setPrefix("tmp/")
    Iterator.continually(ve.listObjectsType2(input))
      .takeWhile(l => l.getContents != null && l.getContents.asScala.nonEmpty)
      .flatMap(l => l.getContents.asScala)
      .map(o => new DeleteObjectInput().setBucket(VE_BUCKET).setKey(o.getKey))
      .foreach: o =>
        ve.deleteObject(o)
        println("Deleted: " + o.getKey)
}

package oss_inner_ali {
  import com.aliyun.sdk.service.oss2.OSSClient
  import com.aliyun.sdk.service.oss2.credentials.StaticCredentialsProvider
  import com.aliyun.sdk.service.oss2.models.PutObjectRequest
  import com.aliyun.sdk.service.oss2.transport.BinaryData
  import zrkn.oss.{ALI_AK, ALI_BUCKET, ALI_REGION, ALI_SK}

  lazy val ali = OSSClient.newBuilder
    .credentialsProvider(new StaticCredentialsProvider(
      ALI_AK,
      ALI_SK
    )).region(ALI_REGION)

  def upload(data: Array[Byte], postfix: String = "") =
    val client = ali.build()
    try
      val key = "tmp/" + java.util.UUID.randomUUID.toString + postfix
      val result = client.putObject:
        PutObjectRequest.newBuilder
          .bucket(ALI_BUCKET)
          .objectAcl("public-read")
          .key(key)
          .body(BinaryData.fromBytes(data))
          .build
      s"https://$ALI_BUCKET.oss-$ALI_REGION.aliyuncs.com/$key"
    finally
      if (client != null)
        client.close()
}

package object oss:
  var VE_AK = ""
  var VE_SK = ""
  var VE_REGION = "cn-beijing"
  var VE_ENDPOINT = "tos-cn-beijing.volces.com"
  var VE_BUCKET = ""

  // 如果要上传本地文件，推荐使用 uploadVE(os.read.bytes(file))
  // 如果上传url指定的文件，推荐使用 uploadVE(requests.get(url).bytes)
  def uploadVE(file: Array[Byte], postfix: String = ""): String = oss_inner_ve.upload(file, postfix)
  def clearVE(): Unit = oss_inner_ve.clear()

  var ALI_REGION = "cn-qingdao"
  var ALI_AK = ""
  var ALI_SK = ""
  var ALI_BUCKET = ""

  def uploadAli(data: Array[Byte], postfix: String = "") =
    oss_inner_ali.upload(data, postfix)
