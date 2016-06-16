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

package fr.petrus.lib.core.processes.results;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import fr.petrus.lib.core.i18n.TextI18n;

/**
 * The base implementation of the {@link ProcessResults} interface, implementing the base methods
 *
 * @param <S> the type parameter
 * @param <E> the type parameter
 * @author Pierre Sagne
 * @since 11.05.2016
 */
public class BaseProcessResults<S, E> implements ProcessResults<S,E> {

    /**
     * The {@code TextI18n} implementation.
     */
    protected TextI18n textI18n;

    /**
     * The successful results.
     */
    protected List<S> success = null;

    /**
     * The elements which were skipped.
     */
    protected List<S> skipped = null;

    /**
     * The failed results.
     */
    protected List<FailedResult<E>> errors = null;

    /**
     * Creates a new {@code BaseProcessResults}.
     *
     * @param textI18n a {@code TextI18n} instance
     * @param success  true if this {@code BaseProcessResults} should have a list of successful results
     * @param skipped  true if this {@code BaseProcessResults} should have a list of skipped results
     * @param errors   true if this {@code BaseProcessResults} should have a list of failed results
     */
    public BaseProcessResults(TextI18n textI18n, boolean success, boolean skipped, boolean errors) {
        this.textI18n = textI18n;
        if (success) {
            this.success = new ArrayList<>();
        }
        if (skipped) {
            this.skipped = new ArrayList<>();
        }
        if (errors) {
            this.errors = new ArrayList<>();
        }
    }

    @Override
    public void addResults(Collection<S> success, Collection<FailedResult<E>> errors) {
        addResults(success, null, errors);
    }

    @Override
    public void addResults(Collection<S> success, Collection<S> skipped, Collection<FailedResult<E>> errors) {
        if (null!=this.success && null!=success) {
            this.success.addAll(success);
        }
        if (null!=this.skipped && null!=skipped) {
            this.skipped.addAll(skipped);
        }
        if (null!=this.errors && null!=errors) {
            this.errors.addAll(errors);
        }
    }

    @Override
    public List<S> getSuccessResultsList() {
        return success;
    }

    @Override
    public List<S> getSkippedResultsList() {
        return skipped;
    }

    @Override
    public List<FailedResult<E>> getErrorResultsList() {
        return errors;
    }

    @Override
    public List<String[]> getResultsTexts(ResultsType resultsType) {
        List<String[]> results = new ArrayList<>();
        for (int i = 0; i < getResultsCount(resultsType); i++) {
            results.add(getResultColumns(resultsType, i));
        }
        return results;
    }

    @Override
    public int getResultsCount(ResultsType resultsType) {
        if (null != resultsType) {
            switch (resultsType) {
                case Success:
                    if (null != success) {
                        return success.size();
                    }
                    break;
                case Skipped:
                    if (null != skipped) {
                        return skipped.size();
                    }
                    break;
                case Errors:
                    if (null != errors) {
                        return errors.size();
                    }
                    break;
            }
        }
        return 0;
    }

    @Override
    public int getResultsColumnsCount(ResultsType resultsType) {
        return 0;
    }

    @Override
    public String[] getResultColumns(ResultsType resultsType, int i) {
        return new String[0];
    }
}
