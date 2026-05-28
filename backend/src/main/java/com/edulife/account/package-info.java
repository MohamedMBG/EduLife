/**
 * Self-service account management (delete-my-account, Play Store mandate).
 * Lives outside the profiles module because account lifecycle and the user's profile
 * are distinct concerns and the delete flow also drives Firebase Admin.
 */
package com.edulife.account;
