/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package javafxpert.conceptmap.alexa.model;


/**
 * Represents the info in an item
 */
public class ItemInfo {
  private String id;
  private String label;
  private String picture;

  public ItemInfo() {
  }

  public ItemInfo(String id, String label, String picture) {
    this.id = id;
    this.label = label;
    this.picture = picture;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getPicture() {
    return picture;
  }

  public void setPicture(String picture) {
    this.picture = picture;
  }

  @java.lang.Override
  public java.lang.String toString() {
    return "ItemInfo{" +
        "id='" + id + '\'' +
        ", label='" + label + '\'' +
        ", picture='" + picture + '\'' +
        '}';
  }
}
