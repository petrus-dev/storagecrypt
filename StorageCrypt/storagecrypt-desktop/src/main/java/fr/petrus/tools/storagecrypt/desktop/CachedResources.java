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

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;

import fr.petrus.lib.core.Constants;
import fr.petrus.lib.core.utils.StreamUtils;

/**
 * The class which loads and caches the resources contained in a HTML page embedded in the
 * application.
 *
 * @author Pierre Sagne
 * @since 18.12.2015
 */
public class CachedResources {

    /**
     * The class which holds the data about the resources.
     */
    public static class ResourcesDescriptor {

        /**
         * The class which holds the data about one resource batch.
         */
        public static class Resource {

            /**
             * The path of the resource file.
             */
            public String resource_path = null;

            /**
             * The path of the cached resource file.
             */
            public String cache_path = null;

            /**
             * The names of the files.
             */
            public List<String> files = null;
        }

        /**
         * The names of the HTML files using this {@code CachedResources}.
         */
        public List<String> indexes = null;

        /**
         * The resources to cache.
         */
        public List<Resource> resources = null;
    }

    private Resources resources;
    private ResourcesDescriptor resourcesDescriptor;

    /**
     * Creates a new {@code CachedResources} instance.
     *
     * @param resources the {@code Resources} instance used to load resources
     */
    public CachedResources(Resources resources) {
        this.resources = resources;
        resourcesDescriptor = null;
    }

    /**
     * Loads data from the file with the given {@code resourcePath}.
     *
     * @param resourcePath the path of the JSON file containing the definition of this
     *                     {@code CachedResources}
     */
    public void load(String resourcePath) {
        InputStream in = resources.getResourceAsStream(resourcePath);
        Gson gson = new Gson();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        resourcesDescriptor = gson.fromJson(reader, ResourcesDescriptor.class);
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Extracts the resources of this {@code CachedResources} to the given {@code destinationFolder}.
     *
     * @param destinationFolder the folder where to extract the resources
     * @throws IOException if an error occurs when reading or writing the resources
     */
    public void extractResourcesTo(File destinationFolder) throws IOException {
        if (destinationFolder.exists() && destinationFolder.isDirectory()) {
            for (ResourcesDescriptor.Resource resource : resourcesDescriptor.resources) {
                File dstResFolder = buildPath(destinationFolder, resource.cache_path);
                dstResFolder.mkdirs();
                for (String fileName : resource.files) {
                    String resourcePath = resource.resource_path+"/"+fileName;
                    File dstFile = new File(dstResFolder, fileName);
                    InputStream in = resources.getResourceAsStream(resourcePath);
                    OutputStream out = new FileOutputStream(dstFile);
                    StreamUtils.copy(out, in, Constants.FILE.BUFFER_SIZE, null);
                }
            }
        }
    }

    /**
     * Returns the file associated to the given {@code fileName}, into the given {@code baseFolder}.
     *
     * @param baseFolder the base folder where the index file is extracted
     * @param fileName   the name of the index file
     * @return the extracted index file
     */
    public File getIndexFile(File baseFolder, String fileName) {
        for (String index : resourcesDescriptor.indexes) {
            String[] indexPathElements = index.split("/");
            if (fileName.equals(indexPathElements[indexPathElements.length - 1])) {
                return buildPath(baseFolder, index);
            }
        }
        return null;
    }

    private static File buildPath(File baseFolder, String subPath) {
        String[] subPathElements = subPath.split("/");
        File path = baseFolder;
        for (String subPathElement : subPathElements) {
            path = new File(path, subPathElement);
        }
        return path;
    }
}
