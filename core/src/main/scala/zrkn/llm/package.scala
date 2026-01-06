package zrkn

import dijon.*

package object llm:

  var ARK_TOKEN = ""
  var ARK_MODEL = "doubao-seed-1-6-251015"
  var ARK_REGION = "cn-beijing"

  var ALI_TOKEN = ""
  var ALI_MODEL = "qwen3-vl-flash"

  var DEEPSEEK_TOKEN = ""

  def llmImageAli(prompt: String,
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
    val data = s"""{
        "model": "$ALI_MODEL",
        "max_completion_tokens": $maxCompletion,
        "enable_thinking": false,
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
                  "text": ${dijon.Json(prompt)},
                  "type": "text"
              }
            ],
            "role": "user"
          }
        ]
      }"""
    println(s"Request body is $data")
    val respText = requests.post(s"https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions", data = data,
    headers = Map(
        "Content-Type" -> "application/json",
        "Authorization" -> s"Bearer $ALI_TOKEN"
      )
    ).text()
    parse(respText).choices(0).message.content.asString.get

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
    val respText = requests.post(s"https://ark.$ARK_REGION.volces.com/api/v3/chat/completions",
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
                  "text": ${dijon.Json(prompt)},
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
                  systemPrompt: String = "",
                  maxCompletion: Int = 65535,
                  thinking: Boolean = false,
                  temperature: Double = 1.3,
                 ): String =
    val respText = requests.post(s"https://api.deepseek.com/chat/completions",
      data = s"""{
        "model": "${if thinking then "deepseek-reasoner" else "deepseek-chat"}",
        "max_completion_tokens": $maxCompletion,
        "temperature": $temperature,
        "messages": [
          ${if systemPrompt.isEmpty then "" else s"""{
            "content": ${dijon.Json(systemPrompt)},
            "role": "system"
          },"""}
          {
            "content": ${dijon.Json(prompt)},
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
