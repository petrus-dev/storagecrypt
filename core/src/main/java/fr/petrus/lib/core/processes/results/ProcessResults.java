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

import java.util.Collection;
import java.util.List;

import fr.petrus.lib.core.cloud.Account;

/**
 * This interface is used to report the results of the various {@link Process} implementations.
 *
 * @param <S> the type of successful result
 * @param <E> the type of exception for failed results
 *
 * @author Pierre Sagne
 * @since 11.05.2016
 */
public interface ProcessResults<S, E> {
    /**
     * The types of results handled by this interface
     */
    enum ResultsType {
        /**
         * The successful results.
         */
        Success,

        /**
         * The elements which were skipped.
         */
        Skipped,

        /**
         * The failed results.
         */
        Errors}

    /**
     * This method should be called at the end of a {@code Process} execution to report its results.
     *
     * @param success the successful results
     * @param errors  the failed results
     */
    void addResults(Collection<S> success, Collection<FailedResult<E>> errors);

    /**
     * This method should be called at the end of a {@code Process} execution to report its results.
     *
     * @param success the successful results
     * @param skipped the elements which were skipped
     * @param errors  the failed results
     */
    void addResults(Collection<S> success, Collection<S> skipped, Collection<FailedResult<E>> errors);

    /**
     * Returns the successful results.
     *
     * @return a list containing all the elements which were successfully processed
     */
    List<S> getSuccessResultsList();

    /**
     * Returns the elements which were skipped.
     *
     * @return a list containing all the elements which were skipped
     */
    List<S> getSkippedResultsList();

    /**
     * Returns the failed results.
     *
     * @return a list containing all the elements which processing failed
     */
    List<FailedResult<E>> getErrorResultsList();

    /**
     * Returns a list containing the given {@code resultsType}, formatted as Strings arrays.
     *
     * @param resultsType the results type
     * @return the list containing the given {@code resultsType}, formatted as String arrays
     */
    List<String[]> getResultsTexts(ResultsType resultsType);

    /**
     * Returns the number of the list of the given {@code resultsType}.
     *
     * @param resultsType the results type
     * @return the number of the list of the given {@code resultsType}
     */
    int getResultsCount(ResultsType resultsType);

    /**
     * Returns the number of Strings (columns) of each result of the list returned by
     * {@link ProcessResults#getResultsTexts} or by {@link ProcessResults#getResultColumns}.
     *
     * @param resultsType the results type
     * @return the number of Strings (columns)
     */
    int getResultsColumnsCount(ResultsType resultsType);

    /**
     * Returns the types of the different columns for the given {@code resultsType}.
     *
     * @param resultsType the results type
     * @return the types of the different columns
     */
    ColumnType[] getResultsColumnsTypes(ResultsType resultsType);

    /**
     * Returns the {@code i}th result of the given {@code resultType} list as a String array.
     *
     * @param resultsType the results type
     * @param i           the index of the result
     * @return the String array for the {@code i}th result of the given {@code resultType} list
     */
    String[] getResultColumns(ResultsType resultsType, int i);

    /**
     * Returns the accounts for which an Oauth Error occured.
     *
     * @return the accounts for which an Oauth Error occured
     */
    Collection<Account> getOauthErrorAccounts();
}
