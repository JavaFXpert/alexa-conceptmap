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
package javafxpert.conceptmap.alexa;

import java.util.List;
import java.util.ArrayList;

/**
 * Represents the claims related to an item
 */
public class ClaimsInfo {
  private String pictureUrl;
  private List<String> itemLabels = new ArrayList<>();

  public ClaimsInfo() {
  }

  public ClaimsInfo(String pictureUrl, List<String> itemNames) {
    this.pictureUrl = pictureUrl;
    this.itemLabels = itemLabels;
  }

  public String getPictureUrl() {
    return pictureUrl;
  }

  public void setPictureUrl(String pictureUrl) {
    this.pictureUrl = pictureUrl;
  }

  public List<String> getItemLabels() {
    return itemLabels;
  }

  public void setItemLabels(List<String> itemLabels) {
    this.itemLabels = itemLabels;
  }

  public String toItemLabelsSpeech() {
    StringBuffer itemLabelsSpeech = new StringBuffer();
    if (itemLabels.size() > 0) {
      for (int i = 0; i < itemLabels.size(); i++) {
        itemLabelsSpeech.append(itemLabels.get(i) + ",\n");
        if ((itemLabels.size() > 1) && (i == itemLabels.size() - 2)) {
          itemLabelsSpeech.append(" and ");
        }
      }
    }
    return itemLabelsSpeech.toString();
  }

  @java.lang.Override
  public java.lang.String toString() {
    return "ClaimsInfo{" +
        "pictureUrl='" + pictureUrl + '\'' +
        ", itemLabels=" + itemLabels +
        '}';
  }
}
