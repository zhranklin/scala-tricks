package zrkn.op

/**
 * Created by 张武(zhangwu@corp.netease.com) at 2020/9/6
 */
trait CirceUtils {

  def parseJson(input : java.lang.String) = io.circe.parser.parse(input)
  def parseYaml(yaml : java.io.Reader) = io.circe.yaml.parser.parseDocuments(yaml)
  def parseYaml(yaml : String) = io.circe.yaml.parser.parseDocuments(yaml)

}
