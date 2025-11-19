package zrkn

import dijon.*

package object llm {

  var ARK_TOKEN = ""
  var ARK_MODEL = "doubao-seed-1-6-251015"
  var REGION = "cn-beijing"

  var DEEPSEEK_TOKEN = ""

  def llmImage(prompt: String,
               imageUrl: String,
               jsonSchema: String = null,
               maxCompletion: Int = 65535
              ): String =
    val response_format =
      if jsonSchema != null && jsonSchema.nonEmpty then
        s""" "response_format": {
          "type": "json_schema",
          "json_schema": {
            "name": "ticket_info",
            "schema": $jsonSchema,
            "strict": true
          }
        },"""
      else ""
    val respText = requests.post(s"https://ark.$REGION.volces.com/api/v3/chat/completions",
      data = s"""{
        "model": "$ARK_MODEL",
        "max_completion_tokens": $maxCompletion,
        "reasoning_effort": "minimal",
        $response_format
        "messages": [
          {
            "content": [
              {
                "image_url": {
                  "url": "$imageUrl"
                },
                "type": "image_url"
              },
              {
                  "text": ${io.circe.Json.fromString(prompt)},
                  "type": "text"
              }
            ],
            "role": "user"
          }
        ]
      }""",
      headers = Map(
        "Content-Type" -> "application/json",
        "Authorization" -> s"Bearer $ARK_TOKEN"
      )
    ).text()
    parse(respText).choices(0).message.content.asString.get
  def askDeepseek(prompt: String,
                  json: Boolean = false,
                  maxCompletion: Int = 65535,
                 ): String =
    val respText = requests.post(s"https://api.deepseek.com/chat/completions",
      data = s"""{
        "model": "deepseek-chat",
        "max_completion_tokens": $maxCompletion,
        "messages": [
          {
            "content": ${io.circe.Json.fromString(prompt)},
            "role": "user"
          }
        ],
        ${if json then """"response_format": {"type": "json_object"},""" else ""}
        "stream": false
      }""",
      headers = Map(
        "Content-Type" -> "application/json",
        "Authorization" -> s"Bearer $DEEPSEEK_TOKEN"
      )
    ).text()
    parse(respText).choices(0).message.content.asString.get
}
