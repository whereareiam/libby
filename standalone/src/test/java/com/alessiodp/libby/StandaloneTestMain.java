package com.alessiodp.libby;

import com.alessiodp.libby.logging.adapters.JDKLogAdapter;
import org.apache.commons.lang3.StringUtils;

import java.util.logging.Logger;

public class StandaloneTestMain {

    public static void main(String[] args) throws Exception {
        StandaloneLibraryManager libraryManager;
        libraryManager = new StandaloneLibraryManager(
                new JDKLogAdapter(Logger.getLogger("LibraryManagerMock")),
                LibbyTestProperties.generateDownloadFolder(),
                "libs"
        );

        try {
            Class.forName(TestUtils.STRING_UTILS_CLASS);
            throw new Exception("StringUtils already present.");
        } catch (Throwable ignore) {
            // The class shouldn't be present before calling loadLibrary
        }

        libraryManager.addMavenCentral();
        libraryManager.loadLibrary(TestUtils.APACHE_COMMONS_LANG3);
        Class.forName(TestUtils.STRING_UTILS_CLASS);

        if (!capitalize("this is a phrase").equals("This is a phrase")) {
            throw new Exception("StringUtils.capitalize(...) returned a wrong result.");
        }
    }

    private static String capitalize(String input) {
        return StringUtils.capitalize(input);
    }
}
