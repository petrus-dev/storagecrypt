/*
 *  Copyright Pierre Sagne (12 december 2014)
 *
 * petrus.dev.fr@gmail.com
 *
 * This software is a computer program whose purpose is to encrypt and
 * synchronize files on the cloud.
 *
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 *
 */

package fr.petrus.tools.storagecrypt.desktop;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

/**
 * This class helps access the resources of the application.
 *
 * @author Pierre Sagne
 * @since 12.09.2015
 */
public class Resources {
    private HashMap<String, Image> loadedImages;

    /**
     * Creates a new {@code Resources} instance.
     */
    public Resources() {
        loadedImages = new HashMap<>();
    }

    /**
     * Returns an InputStream from the given {@code resourcePath}.
     *
     * @param resourcePath the path of the resource to retrieve
     * @return the {@code InputStream} for the given {@code resourcePath}
     */
    public InputStream getResourceAsStream(String resourcePath) {
        return getClass().getResourceAsStream(resourcePath);
    }

    /**
     * Loads and returns an {@code Image} from the given {@code resourcePath}.
     *
     * @param resourcePath the path of the resource to retrieve
     * @return the {@code Image} for the given {@code resourcePath}
     */
    public Image loadImage(String resourcePath) {
        if (!loadedImages.containsKey(resourcePath)) {
            Image image = new Image(Display.getCurrent(), getResourceAsStream(resourcePath));
            loadedImages.put(resourcePath, image);
        }
        return loadedImages.get(resourcePath);
    }

    /**
     * Loads and returns an {@code String} from the given {@code resourcePath}.
     *
     * @param resourcePath the path of the resource to retrieve
     * @return the {@code String} for the given {@code resourcePath}
     */
    public String loadText(String resourcePath) {
        InputStream inputStream = getResourceAsStream(resourcePath);
        if (null==inputStream) {
            return null;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder  stringBuilder = new StringBuilder();
        String ls = System.getProperty("line.separator");

        try {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append(ls);
            }
        } catch (IOException e) {
            return null;
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return stringBuilder.toString();
    }
}
