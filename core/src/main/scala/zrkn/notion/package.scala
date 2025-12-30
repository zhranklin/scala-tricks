package zrkn

package object notion:
  import zrkn.op._
  // npm install -g @tryfabric/martian
  def markdownToBlocks(markdown: String): collection.Seq[dijon.SomeJson] =
    val input = dijon.Json(markdown)
    val script = s"""const options = {notionLimits: {truncate: false}};
                    |const result = require("@tryfabric/martian").markdownToBlocks($input, options);
                    |console.log(JSON.stringify(result))""".stripMargin
    dijon.parse:
      !.__env("NODE_PATH" -> !.npm.root.`-g`.!!.trim).node.`-e`.__(script).!!
    .toSeq
