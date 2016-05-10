/**
    Copyright 2014-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.

    Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with the License. A copy of the License is located at

        http://aws.amazon.com/apache2.0/

    or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package javafxpert.conceptmap.alexa;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SessionEndedRequest;
import com.amazon.speech.speechlet.SessionStartedRequest;
import com.amazon.speech.speechlet.Speechlet;
import com.amazon.speech.speechlet.SpeechletException;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.ui.OutputSpeech;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.SsmlOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;
import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;
import com.amazonaws.util.json.JSONTokener;

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

        return newAskResponse(speechOutput, speechOutput);
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
