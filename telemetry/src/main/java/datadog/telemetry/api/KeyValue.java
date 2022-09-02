/**
 * Datadog Telemetry API Generated by Openapi Generator
 * https://github.com/openapitools/openapi-generator
 *
 * <p>The version of the OpenAPI document: 1.0.0
 *
 * <p>NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 */
package datadog.telemetry.api;

public class KeyValue {

  @com.squareup.moshi.Json(name = "name")
  private String name;

  @com.squareup.moshi.Json(name = "value")
  private Object value;

  /**
   * Get name
   *
   * @return name
   */
  public String getName() {
    return name;
  }

  /** Set name */
  public void setName(String name) {
    this.name = name;
  }

  public KeyValue name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get value
   *
   * @return value
   */
  public Object getValue() {
    return value;
  }

  /** Set value */
  public void setValue(Object value) {
    this.value = value;
  }

  public KeyValue value(Object value) {
    this.value = value;
    return this;
  }

  /** Create a string representation of this pojo. */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class KeyValue {\n");

    sb.append("    name: ").append(name).append("\n");
    sb.append("    value: ").append(value).append("\n");
    sb.append("}");
    return sb.toString();
  }
}
