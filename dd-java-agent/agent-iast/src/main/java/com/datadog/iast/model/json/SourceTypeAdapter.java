package com.datadog.iast.model.json;

import com.datadog.iast.model.SourceType;
import com.squareup.moshi.FromJson;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.ToJson;
import java.io.IOException;

class SourceTypeAdapter {

  @ToJson
  void toJson(JsonWriter writer, @SourceTypeString byte value) throws IOException {
    final String stringValue = SourceType.toString(value);
    if (stringValue == null) {
      writer.nullValue();
      return;
    }
    writer.value(stringValue);
  }

  @FromJson
  @SourceTypeString
  byte fromJson(JsonReader reader) {
    throw new UnsupportedOperationException("SourceType deserialization not supported");
  }
}
