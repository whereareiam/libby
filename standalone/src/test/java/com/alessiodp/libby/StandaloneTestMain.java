package com.alessiodp.libby;

import com.alessiodp.libby.logging.adapters.JDKLogAdapter;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.logging.Logger;

public class StandaloneTestMain {

    public static void main(String[] args) {
        StandaloneLibraryManager libraryManager;
        try {
            libraryManager = new StandaloneLibraryManager(new JDKLogAdapter(Logger.getLogger("LibraryManagerMock")),
                    LibbyTestProperties.generateDownloadFolder(),
                    "libs");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
            return;
        }

        try {
            Class.forName(TestUtils.STRING_UTILS_CLASS);
            new RuntimeException("StringUtils already present.").printStackTrace();
            System.exit(1);
        } catch (Throwable ignore) {
            // The class shouldn't be present before calling loadLibrary
        }

        try {
            libraryManager.addMavenCentral();
            libraryManager.loadLibrary(TestUtils.APACHE_COMMONS_LANG3);
            Class.forName(TestUtils.STRING_UTILS_CLASS);

            if (!capitalize("this is a phrase").equals("This is a phrase")) {
                throw new RuntimeException("StringUtils.capitalize(...) returned a wrong result.");
            }
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }

        System.exit(0); // All fine
    }

    private static String capitalize(String input) {
        return StringUtils.capitalize(input);
    }
}
