package com.ion.validator.interfaces;

import com.ion.validator.Validate;

/**
 * Implemented to perform custom error actions rather
 * than using {@link android.widget.TextView#setError(CharSequence)}
 */
public interface CustomErrorNotification {
  /**
   * Called when an error has been detected.
   *
   * @param validate The specific {@link com.ion.validator.Validate}
   *                 that is under examination.
   */
  public void onInvalid(Validate validate);

  /**
   * Called when a field is confirmed to be valid.
   *
   * @param validate  The specific {@link com.ion.validator.Validate}
   *                  that is under examination.
   */
  public void onValid(Validate validate);
}
