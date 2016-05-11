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

import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.*;
import com.amazon.speech.ui.*;
import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;
import com.amazonaws.util.json.JSONTokener;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
//import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.ParseException;

import javafxpert.conceptmap.alexa.model.ClaimsInfo;
import javafxpert.conceptmap.alexa.model.ItemInfo;


/**
 * This sample shows how to create a Lambda function for handling Alexa Skill requests that:
 * <ul>
 * <li><b>Web service</b>: communicate with an external web service to get tide data from NOAA
 * CO-OPS API (http://tidesandcurrents.noaa.gov/api/)</li>
 * <li><b>Multiple optional slots</b>: has 2 slots (city and date), where the user can provide 0, 1,
 * or 2 values, and assumes defaults for the unprovided values</li>
 * <li><b>DATE slot</b>: demonstrates date handling and formatted date responses appropriate for
 * speech</li>
 * <li><b>Custom slot type</b>: demonstrates using custom slot types to handle a finite set of known values</li>
 * <li><b>SSML</b>: Using SSML tags to control how Alexa renders the text-to-speech</li>
 * <li><b>Pre-recorded audio</b>: Uses the SSML 'audio' tag to include an ocean wave sound in the welcome response.</li>
 * <p>
 * - Dialog and Session state: Handles two models, both a one-shot ask and tell model, and a
 * multi-turn dialog model. If the user provides an incorrect slot in a one-shot model, it will
 * direct to the dialog model. See the examples section for sample interactions of these models.
 * </ul>
 * <p>
 * <h2>Examples</h2>
 * <p>
 * <b>One-shot model</b>
 * <p>
 * User: "Alexa, ask Tide Pooler when is the high tide in Seattle on Saturday" Alexa: "Saturday June
 * 20th in Seattle the first high tide will be around 7:18 am, and will peak at ...""
 * <p>
 * <b>Dialog model</b>
 * <p>
 * User: "Alexa, open Tide Pooler"
 * <p>
 * Alexa: "Welcome to Tide Pooler. Which city would you like tide information for?"
 * <p>
 * User: "Seattle"
 * <p>
 * Alexa: "For which date?"
 * <p>
 * User: "this Saturday"
 * <p>
 * Alexa: "Saturday June 20th in Seattle the first high tide will be around 7:18 am, and will peak
 * at ..."
 */
public class ConceptMapSpeechlet implements Speechlet {
  private static final Logger log = LoggerFactory.getLogger(ConceptMapSpeechlet.class);

  private static final String SLOT_RELATIONSHIP = "Relationship";
  private static final String SLOT_ITEM = "Item";
  private static final String TRAVERSAL_ENDPOINT = "https://conceptmap.cfapps.io/traversal";
  private static final String ID_LOCATOR_ENDPOINT = "https://conceptmap.cfapps.io/idlocator";

  @Override
  public void onSessionStarted(final SessionStartedRequest request, final Session session)
          throws SpeechletException {
      log.info("onSessionStarted requestId={}, sessionId={}", request.getRequestId(),
              session.getSessionId());

      // any initialization logic goes here
  }

  @Override
  public SpeechletResponse onLaunch(final LaunchRequest request, final Session session)
          throws SpeechletException {
      log.info("onLaunch requestId={}, sessionId={}", request.getRequestId(),
              session.getSessionId());

      return getWelcomeResponse();
  }

  @Override
  public SpeechletResponse onIntent(final IntentRequest request, final Session session)
          throws SpeechletException {
      log.info("onIntent requestId={}, sessionId={}", request.getRequestId(),
              session.getSessionId());

      Intent intent = request.getIntent();
      String intentName = intent.getName();

      Slot itemSlot = intent.getSlot(SLOT_ITEM);
      Slot relSlot = intent.getSlot(SLOT_RELATIONSHIP);
      if (itemSlot != null && itemSlot.getValue() != null) {
          log.info("I received an Item request: " + itemSlot.getValue());
      }
      else if (relSlot != null && relSlot.getValue() != null) {
          log.info("I received a Relationship request: " + relSlot.getValue());
      }
      else {
          log.info("I'm not sure if I received a request");
      }


      if ("OneshotClaimsIntent".equals(intentName)) {
          return handleOneshotTideRequest(intent, session);
      }

      /*
      else if ("AMAZON.HelpIntent".equals(intentName)) {
          return handleHelpRequest();
      }
      */

      else if ("AMAZON.StopIntent".equals(intentName)) {
          PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
          outputSpeech.setText("Goodbye");

          return SpeechletResponse.newTellResponse(outputSpeech);
      } else if ("AMAZON.CancelIntent".equals(intentName)) {
          PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
          outputSpeech.setText("Goodbye");

          return SpeechletResponse.newTellResponse(outputSpeech);
      } else {
          throw new SpeechletException("Invalid Intent");
      }
  }

  @Override
  public void onSessionEnded(final SessionEndedRequest request, final Session session)
          throws SpeechletException {
      log.info("onSessionEnded requestId={}, sessionId={}", request.getRequestId(),
              session.getSessionId());
  }


  private SpeechletResponse getWelcomeResponse() {
      String whichItemRelPrompt = "Which item and relationship would you like claims for?";
      String speechOutput = "<speak>"
          + "Welcome to Concept Map. "
          + whichItemRelPrompt
          + "</speak>";
      String repromptText =
          "I can lead you through providing an item and "
              + "relationship to get claims, "
              + "or you can simply open Concept Map and ask a question like, "
              + "what teams has Lionel Messi played on. ";

      return newAskResponse(speechOutput, true, repromptText, false);
  }

  /**
   * This handles the one-shot interaction, where the user utters a phrase like: 'Alexa, open Tide
   * Pooler and get tide information for Seattle on Saturday'. If there is an error in a slot,
   * this will guide the user to the dialog approach.
   */
  private SpeechletResponse handleOneshotTideRequest(final Intent intent, final Session session) {
      Slot itemSlot;
      Slot relSlot;
      String speechOutput;

      try {
          itemSlot = intent.getSlot(SLOT_ITEM);
          relSlot = intent.getSlot(SLOT_RELATIONSHIP);
      } catch (Exception e) {
          // invalid city. move to the dialog
          speechOutput =
                  "I need both an Item and a Relationship";

          // repromptText is the same as the speechOutput
          return newAskResponse(speechOutput, speechOutput);
      }


      // all slots filled, either from the user or by default values. Move to final request
      speechOutput =
          "Item is " + itemSlot.getValue() + "and relationship is " + relSlot.getValue();

    //return makeClaimsRequest("Q615", "P54");
    return makeClaimsRequest(itemSlot.getValue(), relSlot.getValue());
  }

  /**
   * Call a ConceptMap endpoint to retrieve related claims
   *
   * @throws IOException
   */
  //private SpeechletResponse makeClaimsRequest(String itemId, String propId) {
  private SpeechletResponse makeClaimsRequest(String itemValue, String relationshipValue) {
    String speechOutput = "";
    Image image = new Image();

    // Translate requested item and relationship to Q and P numbers
    //String itemId = "Q887401";
    String propId = "P54";
    String itemId = locateItemId(itemValue);
    if (itemId != null && itemId.length() > 0) {

      String queryString =
          String.format("?id=%s&direction=f&prop=%s&depth=1", itemId, propId);


      InputStreamReader inputStream = null;
      BufferedReader bufferedReader = null;
      StringBuilder builder = new StringBuilder();
      try {
        String line;
        URL url = new URL(TRAVERSAL_ENDPOINT + queryString);
        inputStream = new InputStreamReader(url.openStream(), Charset.forName("US-ASCII"));
        bufferedReader = new BufferedReader(inputStream);
        while ((line = bufferedReader.readLine()) != null) {
          builder.append(line);
        }
      } catch (IOException e) {
        // reset builder to a blank string
        builder.setLength(0);
      } finally {
        IOUtils.closeQuietly(inputStream);
        IOUtils.closeQuietly(bufferedReader);

        log.info("builder: " + builder);
      }

      if (builder.length() == 0) {
        speechOutput =
            "Sorry, the Concept Map claims service is experiencing a problem. "
                + "Please try again later.";
      } else {
        try {
          JSONObject claimsResponseObject = new JSONObject(new JSONTokener(builder.toString()));

          if (claimsResponseObject != null) {
            ClaimsInfo claimsInfo = createClaimsInfo(claimsResponseObject, itemId);

            log.info("claimsInfo: " + claimsInfo);

            speechOutput = "Item " + itemValue + " not found";

            if (claimsInfo.getItemLabels().size() > 0) {
              speechOutput = new StringBuilder()
                  .append(itemValue)
                  //.append(claimsInfo.getItemLabels().get(0))
                  .append(" has been a member of ")
                  .append(relationshipValue)
                  .append(" ")
                  .append(claimsInfo.toItemLabelsSpeech())
                  .toString();
            }
            image.setLargeImageUrl(claimsInfo.getPictureUrl());

                  /*
                  speechOutput =
                      new StringBuilder()
                          .append(date.speechValue)
                          .append(" in ")
                          .append(cityStation.speechValue)
                          .append(", the first high tide will be around ")
                          .append(highTideResponse.firstHighTideTime)
                          .append(", and will peak at about ")
                          .append(highTideResponse.firstHighTideHeight)
                          .append(", followed by a low tide at around ")
                          .append(highTideResponse.lowTideTime)
                          .append(" that will be about ")
                          .append(highTideResponse.lowTideHeight)
                          .append(". The second high tide will be around ")
                          .append(highTideResponse.secondHighTideTime)
                          .append(", and will peak at about ")
                          .append(highTideResponse.secondHighTideHeight)
                          .append(".")
                          .toString();
                  */
          }
          //} catch (JSONException | ParseException e) {
        } catch (JSONException e) {
          log.error("Exception occoured while parsing service response.", e);
        }
      }
    }
    else {
      speechOutput = "Couldn't locate an Item ID for item " + itemValue;
    }

    // Create the Simple card content.
    StandardCard card = new StandardCard();
    card.setTitle("Concept Map");
    card.setText(speechOutput);
    card.setImage(image);
    // Create the plain text output
    PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
    outputSpeech.setText(speechOutput);

    return SpeechletResponse.newTellResponse(outputSpeech, card);
  }

  /**
   * Create an object that contains claims info
   */
  private ClaimsInfo createClaimsInfo(JSONObject responseObject, String itemId) throws JSONException { //, ParseException {
    ClaimsInfo claimsInfo = new ClaimsInfo();
    JSONArray items = (JSONArray) responseObject.get("item");

    for (int i = 0; i < items.length(); i++) {
      JSONObject itemInfoJson = (JSONObject) items.get(i);
      ItemInfo itemInfo = new ItemInfo((String)itemInfoJson.get("id"),
          (String)itemInfoJson.get("label"),
          (String)itemInfoJson.get("picture"));
      if (itemInfo.getId().equals(itemId)) {
        claimsInfo.setPictureUrl(itemInfo.getPicture());
      }
      else {
        claimsInfo.getItemLabels().add(itemInfo.getLabel());
      }
    }
    return claimsInfo;
  }

  /**
   * Call a ConceptMap endpoint to get the Item ID for a given article name
   *
   * @throws IOException
   */
  private String locateItemId(String itemValue) {
    String itemId = "";

    String queryString =
        String.format("?name=%s&lang=en", itemValue);
    queryString = queryString.replaceAll(" ", "%20");
    log.info("queryString: " + queryString);

    InputStreamReader inputStream = null;
    BufferedReader bufferedReader = null;
    StringBuilder builder = new StringBuilder();
    try {
      String line;
      URL url = new URL(ID_LOCATOR_ENDPOINT + queryString);

      log.info("locateItemId url: " + url);

      inputStream = new InputStreamReader(url.openStream(), Charset.forName("US-ASCII"));
      bufferedReader = new BufferedReader(inputStream);
      while ((line = bufferedReader.readLine()) != null) {
        builder.append(line);
      }
    } catch (IOException e) {
      log.info("IOException e: " + e);
      // reset builder to a blank string
      builder.setLength(0);
    } finally {
      IOUtils.closeQuietly(inputStream);
      IOUtils.closeQuietly(bufferedReader);

      log.info("locateItemId builder: " + builder);
    }

    if (builder.length() > 0) {
      try {
        JSONObject idResponseObject = new JSONObject(new JSONTokener(builder.toString()));

        if (idResponseObject != null) {
          itemId = (String) idResponseObject.get("itemId");

          log.info("locateItemId itemId: " + itemId);

          if (itemId == null) {
            itemId = "";
          }
        }
      } catch (JSONException e) {
        log.error("Exception occoured while parsing service response.", e);
      }
    }

    return itemId;
  }


  /**
   * Wrapper for creating the Ask response from the input strings with
   * plain text output and reprompt speeches.
   *
   * @param stringOutput
   *            the output to be spoken
   * @param repromptText
   *            the reprompt for if the user doesn't reply or is misunderstood.
   * @return SpeechletResponse the speechlet response
   */
  private SpeechletResponse newAskResponse(String stringOutput, String repromptText) {
      return newAskResponse(stringOutput, false, repromptText, false);
  }

  /**
   * Wrapper for creating the Ask response from the input strings.
   *
   * @param stringOutput
   *            the output to be spoken
   * @param isOutputSsml
   *            whether the output text is of type SSML
   * @param repromptText
   *            the reprompt for if the user doesn't reply or is misunderstood.
   * @param isRepromptSsml
   *            whether the reprompt text is of type SSML
   * @return SpeechletResponse the speechlet response
   */
  private SpeechletResponse newAskResponse(String stringOutput, boolean isOutputSsml,
          String repromptText, boolean isRepromptSsml) {
      OutputSpeech outputSpeech, repromptOutputSpeech;
      if (isOutputSsml) {
          outputSpeech = new SsmlOutputSpeech();
          ((SsmlOutputSpeech) outputSpeech).setSsml(stringOutput);
      } else {
          outputSpeech = new PlainTextOutputSpeech();
          ((PlainTextOutputSpeech) outputSpeech).setText(stringOutput);
      }

      if (isRepromptSsml) {
          repromptOutputSpeech = new SsmlOutputSpeech();
          ((SsmlOutputSpeech) repromptOutputSpeech).setSsml(stringOutput);
      } else {
          repromptOutputSpeech = new PlainTextOutputSpeech();
          ((PlainTextOutputSpeech) repromptOutputSpeech).setText(repromptText);
      }

      Reprompt reprompt = new Reprompt();
      reprompt.setOutputSpeech(repromptOutputSpeech);
      return SpeechletResponse.newAskResponse(outputSpeech, reprompt);
  }


}
