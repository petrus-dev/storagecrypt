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

package fr.petrus.lib.core.cloud.exceptions;

import fr.petrus.lib.core.cloud.Account;

/**
 * Exception thrown when an error occurs when performing Oauth related operations.
 *
 * @author Pierre Sagne
 * @since 15.05.2017
 */
public class OauthException extends Exception {
    /** The possible reasons why the exception can be thrown */
    public enum Reason {
        RefreshTokenInvalidGrant
    }

    /** The reason why the exception is thrown */
    private Reason reason = null;

    /** The account for which the exception is thrown */
    private Account account = null;

    /**
     * Creates a new {@code OauthException} with the specified detail {@code message},
     * the {@code reason} and the given root {@code cause}.
     *
     * @param message the detail message
     * @param reason  the reason for throwing the exception
     * @param account the account for which the exception is thrown
     * @param cause   the root cause (usually from using the underlying API (Retrofit))
     */
    public OauthException(String message, Reason reason, Account account, Throwable cause) {
        super(message, cause);
        this.reason = reason;
        this.account = account;
    }

    /**
     * Creates a new {@code OauthException} with the specified detail {@code message}
     * and the {@code reason} for throwing the exception.
     *
     * @param message the detail message
     * @param reason  the reason for throwing the exception
     * @param account the account for which the exception is thrown
     */
    public OauthException(String message, Reason reason, Account account) {
        super(message);
        this.reason = reason;
        this.account = account;
    }

    /**
     * Returns the reason why the exception was thrown.
     *
     * @return the reason why the exception was thrown
     */
    public Reason getReason() {
        return reason;
    }

    /**
     * Returns the account for which the exception was thrown.
     *
     * @return the account for which the exception was thrown
     */
    public Account getAccount() {
        return account;
    }

    public boolean isRefreshTokenInvalidGrant() {
        return Reason.RefreshTokenInvalidGrant == reason;
    }
}
