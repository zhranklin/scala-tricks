package zrkn

import com.volcengine.tos.{TOSClientConfiguration, TOSV2ClientBuilder}
import com.volcengine.tos.auth.StaticCredentials
import com.volcengine.tos.comm.common.ACLType
import com.volcengine.tos.model.`object`.{ObjectMetaRequestOptions, PutObjectInput}

import java.io.ByteArrayInputStream
import java.util.UUID

package object tos {
  var TOS_AK = ""
  var TOS_SK = ""
  var TOS_REGION = "cn-beijing"
  var TOS_ENDPOINT = "tos-cn-beijing.volces.com"
  var TOS_BUCKET = ""


  lazy private val tos = new TOSV2ClientBuilder().build:
    TOSClientConfiguration.builder()
      .region(TOS_REGION)
      .endpoint(TOS_ENDPOINT)
      .credentials(new StaticCredentials(TOS_AK, TOS_SK))
      .build()

  // 如果要上传本地文件，推荐使用 uploadTOS(os.read.bytes(file))
  // 如果上传url指定的文件，推荐使用 uploadTOS(requests.get(url).bytes)
  def uploadTOS(image: Array[Byte], postfix: String = ""): String =
    val fn = UUID.randomUUID.toString + postfix
    val put = new PutObjectInput()
      .setBucket(TOS_BUCKET)
      .setKey(s"tmp/$fn")
      .setOptions {
        val o = new ObjectMetaRequestOptions
        o.setAclType(ACLType.ACL_PUBLIC_READ)
      }
      .setContent(new ByteArrayInputStream(image))
    val resp = tos.putObject(put)
    s"https://$TOS_BUCKET.$TOS_ENDPOINT/tickets/$fn"

}
