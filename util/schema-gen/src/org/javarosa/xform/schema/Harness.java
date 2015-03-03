/*
 * Copyright (C) 2009 JavaRosa
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.javarosa.xform.schema;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import org.javarosa.core.model.FormDef;
import org.javarosa.xform.parse.XFormParseException;
import org.javarosa.xform.parse.XFormParser;
import org.javarosa.xform.util.XFormUtils;
import org.json.simple.JSONObject;
import org.kxml2.io.KXmlSerializer;
import org.kxml2.kdom.Document;

public class Harness {
    public static void main(String[] args) {
        try {
            if (args.length == 0 || args[0].equals("schema")) {
                FormDef form = loadSchema(args);
                processSchema(form);
            } else if (args[0].equals("summary")) {
                FormDef form = loadSchema(args);
                System.out.println(FormOverview.overview(form));
            } else if (args[0].equals("csvdump")) {
                FormDef form = loadSchema(args);
                System.out.println(FormTranslationFormatter.dumpTranslationsIntoCSV(form));
            } else if (args[0].equals("csvimport")) {
                csvImport(args);
            } else if (args[0].equals("validatemodel")) {
                validateModel(args[1], args[2]);
            } else if (args[0].equals("validate")) {
                validateForm(args);
            } else {
                System.err.println("Usage: java -jar form_translate.jar [validate|schema|summary|csvdump] < form.xml > output");
                System.err.println("or: java -jar form_translate.jar csvimport [delimeter] [encoding] [outcoding] < translations.csv > itextoutput");
                System.err.println("or: java -jar form_translate.jar validatemodel /path/to/xform /path/to/instance");
                System.exit(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            System.exit(0);
        }
    }

    /**
     * Read in form from standard input or filename argument and run it through
     * XForm parser, logging errors along the way.
     *
     * @param args is an String array, where the first entry, if present will be treated as a filename.
     */
    private static void validateForm(String[] args) {
        InputStream inputStream = System.in;

        // If command line args non-empty, treat first entry as filename we
        // open to get the form.
        if (args.length > 1) {
            String formPath = args[1];

            FileInputStream formInput = null;

            try {
                inputStream = new FileInputStream(formPath);
            } catch (FileNotFoundException e) {
                System.err.println("Couldn't find file at: " + formPath);
                System.exit(1);
            }
        }
        PrintStream responseStream = System.out;
        // Redirect output to syserr. We're using sysout for the response, gotta keep it clean
        System.setOut(System.err);

        InputStreamReader isr;
        try {
            isr = new InputStreamReader(inputStream, "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            System.out.println("UTF 8 encoding unavailable, trying default encoding");
            isr = new InputStreamReader(inputStream);
        }

        try {
            JSONReporter reporter = new JSONReporter();
            try {
                XFormParser parser = new XFormParser(isr);
                parser.attachReporter(reporter);
                parser.parse();

                reporter.setPassed();
            } catch (IOException e) {
                // Rethrow this. This is probably a failure of the system, not the form
                reporter.setFailed(e);
                System.err.println("IO Exception while processing form");
                e.printStackTrace();
                System.exit(1);
            } catch (XFormParseException xfpe) {
                reporter.setFailed(xfpe);
            } catch (Exception e) {
                reporter.setFailed(e);
            }

            responseStream.print(reporter.generateJSONReport());
        } finally {
            try {
                isr.close();
                System.exit(0);
            } catch (IOException e) {
                System.err.println("IO Exception while closing stream.");
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    private static void validateModel(String formPath, String modelPath) {
        FileInputStream formInput = null;
        FileInputStream instanceInput = null;

        try {
            formInput = new FileInputStream(formPath);
        } catch (FileNotFoundException e) {
            System.out.println("Couldn't find file at: " + formPath);
            System.exit(1);
        }

        try {
            instanceInput = new FileInputStream(modelPath);
        } catch (FileNotFoundException e) {
            System.out.println("Couldn't find file at: " + modelPath);
            System.exit(1);
        }

        try {
            FormInstanceValidator validator = new FormInstanceValidator(formInput, instanceInput);
            validator.simulateEntryTest();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("Form instance appears to be valid");
        System.exit(0);
    }

    private static void csvImport(String[] args) {
        // TODO: refactor so that instead of passing in args, we just pass in individual arguments
        if (args.length > 1) {
            String delimeter = args[1];
            FormTranslationFormatter.turnTranslationsCSVtoItext(System.in, System.out, delimeter, null, null);
        } else if (args.length > 2) {
            String delimeter = args[1];
            String encoding = args[2];
            FormTranslationFormatter.turnTranslationsCSVtoItext(System.in, System.out, delimeter, encoding, null);
        } else if (args.length > 3) {
            String delimeter = args[1];
            String incoding = args[2];
            String outcoding = args[3];
            FormTranslationFormatter.turnTranslationsCSVtoItext(System.in, System.out, delimeter, incoding, outcoding);
        } else {
            FormTranslationFormatter.turnTranslationsCSVtoItext(System.in, System.out);
        }
        System.exit(0);
    }

    private static FormDef loadSchema(String[] args) {
        InputStream inputStream = System.in;

        // open form file
        if (args.length > 1) {
            String formPath = args[1];

            FileInputStream formInput = null;

            try {
                inputStream = new FileInputStream(formPath);
            } catch (FileNotFoundException e) {
                System.out.println("Couldn't find file at: " + formPath);
                System.exit(1);
            }
        }

        return XFormUtils.getFormFromInputStream(inputStream);
    }

    private static void processSchema(FormDef form) {
        Document schemaDoc = InstanceSchema.generateInstanceSchema(form);
        KXmlSerializer serializer = new KXmlSerializer();
        try {
            serializer.setOutput(System.out, null);
            schemaDoc.write(serializer);
            serializer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
