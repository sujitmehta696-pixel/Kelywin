package com.example.services

import com.example.data.local.AppControlConfigDao
import com.example.data.local.PasswordResetRequestDao
import com.example.data.local.PasswordResetRequestEntity
import com.example.data.local.PayoutMethodDao
import com.example.data.local.PayoutMethodEntity
import com.example.data.local.RememberedCredentials
import com.example.data.local.SessionPreferences
import com.example.data.local.UserDao
import com.example.data.local.UserEntity
import com.example.data.local.WalletTransactionDao
import com.example.data.local.WalletTransactionEntity
import com.example.data.local.WithdrawalRequestDao
import com.example.data.local.WithdrawalRequestEntity
import com.example.shared.model.PasswordResetRequest
import com.example.shared.model.PayoutMethod
import com.example.shared.model.PayoutMethodType
import com.example.shared.model.RequestStatus
import com.example.shared.model.TransactionCategory
import com.example.shared.model.TransactionStatus
import com.example.shared.model.UserAccount
import com.example.shared.model.UserSettings
import com.example.shared.model.WalletTransaction
import com.example.shared.model.WithdrawalRequest
import com.example.shared.security.SecretRecoveryCrypto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.Locale
import java.util.UUID

data class RecoveredPasswordInfo(
    val userId: String,
    val kwId: String,
    val loginIdentifier: String,
    val isEmail: Boolean,
    val countryCode: String,
    val recoveredPassword: String
)

class AuthAndAccountService(
    private val userDao: UserDao,
    private val payoutMethodDao: PayoutMethodDao,
    private val walletTransactionDao: WalletTransactionDao,
    private val withdrawalRequestDao: WithdrawalRequestDao,
    private val passwordResetRequestDao: PasswordResetRequestDao,
    private val appControlConfigDao: AppControlConfigDao,
    private val sessionPreferences: SessionPreferences,
    private val userFirebaseRepository: UserFirebaseRepository? = null
) {
    val activeUserIdFlow: Flow<String?> = sessionPreferences.activeUserIdFlow
    val rememberedCredentialsFlow: Flow<RememberedCredentials> = sessionPreferences.rememberedCredentialsFlow
    val userSettingsFlow: Flow<UserSettings> = sessionPreferences.userSettingsFlow

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val currentUserFlow: Flow<UserAccount?> = activeUserIdFlow.flatMapLatest { uid ->
        if (uid == null) flowOf(null) else userDao.observeUser(uid).map { it?.toDomain() }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val payoutMethodsFlow: Flow<List<PayoutMethod>> = activeUserIdFlow.flatMapLatest { uid ->
        if (uid == null) flowOf(emptyList())
        else payoutMethodDao.observePayoutMethods(uid).map { list -> list.map { it.toDomain() } }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val transactionsFlow: Flow<List<WalletTransaction>> = activeUserIdFlow.flatMapLatest { uid ->
        if (uid == null) flowOf(emptyList())
        else walletTransactionDao.observeTransactions(uid).map { list -> list.map { it.toDomain() } }
    }

    suspend fun registerNoOtp(
        countryCode: String,
        phoneNumber: String,
        password: String,
        confirmPassword: String,
        inviteCode: String,
        acceptedPrivacy: Boolean
    ): Result<UserAccount> {
        val appConfig = appControlConfigDao.getConfig()
        if (appConfig != null && !appConfig.enableRegistration) {
            return Result.failure(IllegalStateException("New user registrations are temporarily paused by Admin."))
        }

        val cleanPhone = phoneNumber.filter { it.isDigit() }
        if (cleanPhone.length < 7) {
            return Result.failure(IllegalArgumentException("Enter a valid phone number (at least 7 digits)."))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }
        if (password != confirmPassword) {
            return Result.failure(IllegalArgumentException("Passwords do not match."))
        }
        if (!acceptedPrivacy) {
            return Result.failure(IllegalArgumentException("Please agree to the Privacy Agreement to continue."))
        }

        val firebaseUid = userFirebaseRepository?.currentFirebaseUid()
        if (userFirebaseRepository != null && firebaseUid == null) {
            return Result.failure(IllegalStateException("Please sign in with Google first to create your cloud-synced account."))
        }

        if (firebaseUid != null) {
            val cloudExisting = userFirebaseRepository.fetchUserProfileEntity(firebaseUid).getOrNull()
            if (cloudExisting != null) {
                return Result.failure(
                    IllegalStateException(
                        "Your Google account already has Kelywin ID ${cloudExisting.kwId} (${cloudExisting.countryCode} ${cloudExisting.phoneNumber}). Please log in or recover your password with your Secret Code."
                    )
                )
            }
        }

        val existingLocal = userDao.getUserByPhone(countryCode, cleanPhone)
            ?: userDao.getUserByPhoneDigits(cleanPhone)
        if (existingLocal != null) {
            return Result.failure(IllegalStateException("This phone number is already registered. Please log in."))
        }

        val now = System.currentTimeMillis()
        val kwId = "KW${(100000..999999).random()}"
        val uid = firebaseUid ?: kwId
        val userInvite = inviteCode.trim().ifBlank { "KW${(10000000..99999999).random()}" }
        val secretCode = SecretRecoveryCrypto.generateSecretCode()
        val secretCodeHash = SecretRecoveryCrypto.hashSecretCode(secretCode)
        val passHash = hashPassword(password)
        val encryptedPass = SecretRecoveryCrypto.encryptPassword(password, secretCode, uid)

        // Every new registered user starts with ₹0 balance; +₹12 bonus unlocks on first approved deposit.
        val newUser = UserEntity(
            userId = uid,
            kwId = kwId,
            countryCode = countryCode,
            phoneNumber = cleanPhone,
            email = "",
            passwordHash = passHash,
            encryptedPassword = encryptedPass,
            secretCode = secretCode,
            secretCodeHash = secretCodeHash,
            inviteCode = userInvite,
            balance = 0.0,
            hasReceivedFirstDepositBonus = false,
            isBlocked = false,
            createdAtMillis = now,
            lastLoginAtMillis = now
        )

        if (userFirebaseRepository != null) {
            userFirebaseRepository.createUserProfile(newUser)
                .onFailure { err -> return Result.failure(err) }
        }

        userDao.upsertUser(newUser)
        sessionPreferences.setActiveUserId(uid)
        sessionPreferences.saveRememberedCredentials(true, countryCode, cleanPhone, password)
        return Result.success(newUser.toDomain())
    }

    suspend fun loginByPhoneNoOtp(
        countryCode: String,
        phoneNumber: String,
        password: String,
        rememberPassword: Boolean
    ): Result<UserAccount> {
        val cleanPhone = phoneNumber.filter { it.isDigit() }
        if (cleanPhone.isEmpty() || password.isEmpty()) {
            return Result.failure(IllegalArgumentException("Enter your phone number and password."))
        }

        val firebaseUid = userFirebaseRepository?.currentFirebaseUid()
        if (firebaseUid != null) {
            userFirebaseRepository.fetchUserProfileEntity(firebaseUid)
        }

        val user = userDao.getUserByPhone(countryCode, cleanPhone)
            ?: userDao.getUserByPhoneDigits(cleanPhone)
            ?: return Result.failure(IllegalStateException("Account not found. Please register first."))

        if (user.isBlocked) {
            return Result.failure(IllegalStateException("This account has been blocked by Admin."))
        }
        if (user.passwordHash != hashPassword(password)) {
            return Result.failure(IllegalArgumentException("Incorrect password."))
        }

        val syncedUser = ensureSecretRecoveryPopulated(user, password)
        val now = System.currentTimeMillis()
        userDao.updateLastLogin(syncedUser.userId, now)
        if (firebaseUid != null && firebaseUid == syncedUser.userId) {
            userFirebaseRepository.updateLastLogin(firebaseUid, now)
        }
        sessionPreferences.setActiveUserId(syncedUser.userId)
        sessionPreferences.saveRememberedCredentials(rememberPassword, syncedUser.countryCode, cleanPhone, password)
        return Result.success(syncedUser.copy(lastLoginAtMillis = now).toDomain())
    }

    suspend fun loginByEmailNoOtp(
        email: String,
        password: String,
        rememberPassword: Boolean
    ): Result<UserAccount> {
        val cleanEmail = email.trim()
        if (cleanEmail.isEmpty() || !cleanEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("Enter a valid email address."))
        }
        if (password.isEmpty()) {
            return Result.failure(IllegalArgumentException("Enter your password."))
        }

        val firebaseUid = userFirebaseRepository?.currentFirebaseUid()
        if (firebaseUid != null) {
            userFirebaseRepository.fetchUserProfileEntity(firebaseUid)
        }

        val user = userDao.getUserByEmail(cleanEmail)
            ?: return Result.failure(IllegalStateException("No account linked to this email yet. Register with phone first."))

        if (user.isBlocked) {
            return Result.failure(IllegalStateException("This account has been blocked by Admin."))
        }
        if (user.passwordHash != hashPassword(password)) {
            return Result.failure(IllegalArgumentException("Incorrect password."))
        }

        val syncedUser = ensureSecretRecoveryPopulated(user, password)
        val now = System.currentTimeMillis()
        userDao.updateLastLogin(syncedUser.userId, now)
        if (firebaseUid != null && firebaseUid == syncedUser.userId) {
            userFirebaseRepository.updateLastLogin(firebaseUid, now)
        }
        sessionPreferences.setActiveUserId(syncedUser.userId)
        sessionPreferences.saveRememberedCredentials(rememberPassword, syncedUser.countryCode, cleanEmail, password)
        return Result.success(syncedUser.copy(lastLoginAtMillis = now).toDomain())
    }

    /**
     * Zero-OTP Secret Recovery Code flow:
     * User enters their Phone / Email / KW ID and fixed Secret Recovery Code (`KW-XXXX-YYYY`) from Settings,
     * and recovers their exact real login password.
     */
    suspend fun recoverPasswordWithSecretCode(
        identifier: String,
        secretCodeInput: String
    ): Result<RecoveredPasswordInfo> {
        val cleanId = identifier.trim()
        val normalizedSecret = SecretRecoveryCrypto.normalizeSecretCode(secretCodeInput)
        if (cleanId.isEmpty()) {
            return Result.failure(IllegalArgumentException("Enter your registered phone number, email, or Kelywin ID."))
        }
        if (normalizedSecret.length < 6) {
            return Result.failure(IllegalArgumentException("Enter your fixed Secret Recovery Code (e.g. KW-XXXX-YYYY)."))
        }

        val firebaseUid = userFirebaseRepository?.currentFirebaseUid()
        if (firebaseUid != null) {
            userFirebaseRepository.fetchUserProfileEntity(firebaseUid)
        }

        val user = when {
            cleanId.contains("@") -> userDao.getUserByEmail(cleanId)
            cleanId.uppercase(Locale.US).startsWith("KW") -> userDao.getUserByKwId(cleanId)
                ?: userDao.getUserById(cleanId)
            else -> userDao.getUserByPhoneDigits(cleanId.filter { it.isDigit() })
        } ?: firebaseUid?.let { uid ->
            userDao.getUserById(uid)?.takeIf { candidate ->
                candidate.kwId.equals(cleanId, ignoreCase = true) ||
                    candidate.email.equals(cleanId, ignoreCase = true) ||
                    candidate.phoneNumber == cleanId.filter { it.isDigit() }
            }
        } ?: return Result.failure(IllegalStateException("No account found matching \"$cleanId\"."))

        val expectedHash = user.secretCodeHash.ifBlank {
            if (user.secretCode.isNotBlank()) SecretRecoveryCrypto.hashSecretCode(user.secretCode) else ""
        }
        if (expectedHash.isBlank() || SecretRecoveryCrypto.hashSecretCode(normalizedSecret) != expectedHash) {
            return Result.failure(IllegalArgumentException("Secret Recovery Code does not match this account."))
        }

        val decrypted = SecretRecoveryCrypto.decryptPassword(
            encryptedBlob = user.encryptedPassword,
            secretCode = normalizedSecret,
            userId = user.userId
        ).getOrElse {
            return Result.failure(
                IllegalStateException("This account password was updated via Admin reset without encrypted recovery. Please request an Admin reset or log in and re-save your password.")
            )
        }

        val isEmailLogin = cleanId.contains("@") && user.email.isNotBlank()
        val preferredIdentifier = when {
            isEmailLogin -> user.email
            user.phoneNumber.isNotBlank() -> user.phoneNumber
            else -> user.email
        }

        return Result.success(
            RecoveredPasswordInfo(
                userId = user.userId,
                kwId = user.kwId.ifBlank { user.userId },
                loginIdentifier = preferredIdentifier,
                isEmail = isEmailLogin,
                countryCode = user.countryCode,
                recoveredPassword = decrypted
            )
        )
    }

    suspend fun submitPasswordResetRequestToAdmin(identifier: String): Result<Unit> {
        val clean = identifier.trim()
        if (clean.isEmpty()) {
            return Result.failure(IllegalArgumentException("Enter your registered phone number or email."))
        }
        val firebaseUid = userFirebaseRepository?.currentFirebaseUid()
        if (firebaseUid != null) {
            userFirebaseRepository.fetchUserProfileEntity(firebaseUid)
        }

        val matchedUser = when {
            clean.contains("@") -> userDao.getUserByEmail(clean)
            clean.uppercase(Locale.US).startsWith("KW") -> userDao.getUserByKwId(clean) ?: userDao.getUserById(clean)
            else -> userDao.getUserByPhoneDigits(clean.filter { it.isDigit() })
        }

        val targetUid = firebaseUid ?: matchedUser?.userId
            ?: return Result.failure(IllegalStateException("Account not found for \"$clean\"."))

        val now = System.currentTimeMillis()
        val reqId = "PWR-${UUID.randomUUID().toString().take(8).uppercase(Locale.US)}"
        val req = PasswordResetRequest(
            id = reqId,
            userId = targetUid,
            identifier = clean,
            status = RequestStatus.PENDING,
            assignedPassword = "",
            deliveryChannel = "",
            createdAtMillis = now,
            resolvedAtMillis = null
        )

        if (userFirebaseRepository != null) {
            userFirebaseRepository.submitPasswordResetRequest(req)
                .onFailure { return Result.failure(it) }
        }

        passwordResetRequestDao.upsert(
            PasswordResetRequestEntity(
                id = req.id,
                userId = req.userId,
                identifier = req.identifier,
                status = req.status.name,
                assignedPassword = req.assignedPassword,
                deliveryChannel = req.deliveryChannel,
                createdAtMillis = req.createdAtMillis,
                resolvedAtMillis = req.resolvedAtMillis
            )
        )
        return Result.success(Unit)
    }

    suspend fun changePasswordNoOtp(
        userId: String,
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ): Result<Unit> {
        val user = userDao.getUserById(userId)
            ?: return Result.failure(IllegalStateException("Session expired. Please log in again."))
        if (user.passwordHash != hashPassword(currentPassword)) {
            return Result.failure(IllegalArgumentException("Current password is incorrect."))
        }
        if (newPassword.length < 6) {
            return Result.failure(IllegalArgumentException("New password must be at least 6 characters."))
        }
        if (newPassword != confirmPassword) {
            return Result.failure(IllegalArgumentException("New passwords do not match."))
        }

        val effectiveSecret = user.secretCode.ifBlank { SecretRecoveryCrypto.generateSecretCode() }
        val secretHash = user.secretCodeHash.ifBlank { SecretRecoveryCrypto.hashSecretCode(effectiveSecret) }
        val newHash = hashPassword(newPassword)
        val newEncrypted = SecretRecoveryCrypto.encryptPassword(newPassword, effectiveSecret, userId)

        if (userFirebaseRepository != null && userFirebaseRepository.currentFirebaseUid() == userId) {
            if (user.secretCode.isBlank()) {
                userFirebaseRepository.updateSecretRecoveryFields(userId, effectiveSecret, secretHash, newEncrypted)
            }
            userFirebaseRepository.updatePasswordCredentials(userId, newHash, newEncrypted)
                .onFailure { return Result.failure(it) }
        }

        if (user.secretCode.isBlank()) {
            userDao.updateSecretRecovery(userId, user.kwId.ifBlank { userId }, effectiveSecret, secretHash, newEncrypted)
        }
        userDao.updatePasswordCredentials(userId, newHash, newEncrypted)
        return Result.success(Unit)
    }

    suspend fun updateEmail(userId: String, email: String): Result<Unit> {
        val clean = email.trim()
        if (!clean.contains("@") || !clean.contains(".")) {
            return Result.failure(IllegalArgumentException("Enter a valid email address."))
        }
        val user = userDao.getUserById(userId) ?: return Result.failure(IllegalStateException("User not found."))
        if (userFirebaseRepository != null && userFirebaseRepository.currentFirebaseUid() == userId) {
            userFirebaseRepository.updateEmail(userId, clean)
                .onFailure { return Result.failure(it) }
        }
        userDao.upsertUser(user.copy(email = clean))
        return Result.success(Unit)
    }

    suspend fun savePayoutMethodNoOtp(
        userId: String,
        existingId: String?,
        type: PayoutMethodType,
        label: String,
        accountHolder: String,
        accountIdentifier: String,
        ifscCode: String,
        makeDefault: Boolean
    ): Result<PayoutMethod> {
        if (accountHolder.isBlank()) {
            return Result.failure(IllegalArgumentException("Account holder name is required."))
        }
        if (type == PayoutMethodType.UPI && !accountIdentifier.contains("@")) {
            return Result.failure(IllegalArgumentException("Enter a valid UPI ID (e.g., name@upi)."))
        }
        if (type == PayoutMethodType.BANK && (accountIdentifier.length < 6 || ifscCode.length < 6)) {
            return Result.failure(IllegalArgumentException("Enter a valid bank account number and IFSC code."))
        }

        val id = existingId ?: "PAY-${UUID.randomUUID().toString().take(8).uppercase(Locale.US)}"
        val now = System.currentTimeMillis()
        val domainMethod = PayoutMethod(
            id = id,
            userId = userId,
            type = type,
            label = label.trim().ifBlank { if (type == PayoutMethodType.UPI) "UPI Wallet" else "Bank Transfer" },
            accountHolder = accountHolder.trim(),
            accountIdentifier = accountIdentifier.trim(),
            ifscCode = ifscCode.trim().uppercase(Locale.US),
            isDefault = makeDefault,
            updatedAtMillis = now
        )

        if (userFirebaseRepository != null && userFirebaseRepository.currentFirebaseUid() == userId) {
            val currentList = payoutMethodDao.observePayoutMethods(userId).first().map { it.toDomain() }
            userFirebaseRepository.savePayoutMethod(userId, domainMethod, currentList)
                .onFailure { return Result.failure(it) }
        }

        if (makeDefault) {
            payoutMethodDao.clearDefaults(userId)
        }
        payoutMethodDao.upsert(
            PayoutMethodEntity(
                id = domainMethod.id,
                userId = domainMethod.userId,
                type = domainMethod.type.name,
                label = domainMethod.label,
                accountHolder = domainMethod.accountHolder,
                accountIdentifier = domainMethod.accountIdentifier,
                ifscCode = domainMethod.ifscCode,
                isDefault = domainMethod.isDefault,
                updatedAtMillis = domainMethod.updatedAtMillis
            )
        )
        return Result.success(domainMethod)
    }

    suspend fun selectDefaultPayoutMethod(userId: String, methodId: String) {
        if (userFirebaseRepository != null && userFirebaseRepository.currentFirebaseUid() == userId) {
            val ids = payoutMethodDao.observePayoutMethods(userId).first().map { it.id }
            userFirebaseRepository.selectDefaultPayoutMethod(userId, methodId, ids)
        }
        payoutMethodDao.clearDefaults(userId)
        payoutMethodDao.setDefault(userId, methodId)
    }

    suspend fun deletePayoutMethod(userId: String, methodId: String) {
        if (userFirebaseRepository != null && userFirebaseRepository.currentFirebaseUid() == userId) {
            userFirebaseRepository.deletePayoutMethod(userId, methodId)
        }
        payoutMethodDao.deleteById(methodId, userId)
    }

    suspend fun submitWithdrawal(
        userId: String,
        amount: Double,
        payoutMethod: PayoutMethod
    ): Result<WalletTransaction> {
        val appConfig = appControlConfigDao.getConfig()
        if (appConfig != null && !appConfig.enableWithdrawals) {
            return Result.failure(IllegalStateException("Withdrawals are temporarily paused by Admin."))
        }
        if (amount < 100.0) {
            return Result.failure(IllegalArgumentException("Minimum withdrawal amount is ₹100.00."))
        }
        val user = userDao.getUserById(userId)
            ?: return Result.failure(IllegalStateException("User session not found."))
        if (user.isBlocked) {
            return Result.failure(IllegalStateException("Your account is blocked."))
        }
        if (amount > user.balance) {
            return Result.failure(IllegalStateException("Insufficient balance for ₹${String.format(Locale.US, "%.2f", amount)} withdrawal."))
        }

        val now = System.currentTimeMillis()
        val reqId = "WD-${UUID.randomUUID().toString().take(8).uppercase(Locale.US)}"
        val reqDomain = WithdrawalRequest(
            id = reqId,
            userId = userId,
            userPhone = "${user.countryCode} ${user.phoneNumber}".trim(),
            amount = amount,
            methodType = payoutMethod.type,
            methodLabel = payoutMethod.label,
            accountHolder = payoutMethod.accountHolder,
            accountIdentifier = payoutMethod.accountIdentifier,
            ifscCode = payoutMethod.ifscCode,
            status = RequestStatus.PENDING,
            adminNote = "",
            createdAtMillis = now,
            processedAtMillis = null
        )
        val txDomain = WalletTransaction(
            id = reqId,
            userId = userId,
            category = TransactionCategory.WITHDRAWAL,
            title = "Withdraw to ${payoutMethod.type.label}",
            subtitle = "${payoutMethod.label} · ${payoutMethod.accountIdentifier}",
            amount = -amount,
            status = TransactionStatus.SUBMITTED,
            createdAtMillis = now
        )

        val updatedBalance = if (userFirebaseRepository != null && userFirebaseRepository.currentFirebaseUid() == userId) {
            userFirebaseRepository.submitWithdrawalCloud(userId, reqDomain, txDomain)
                .getOrElse { return Result.failure(it) }
        } else {
            user.balance - amount
        }

        userDao.updateBalance(userId, updatedBalance)
        withdrawalRequestDao.upsert(
            WithdrawalRequestEntity(
                id = reqDomain.id,
                userId = reqDomain.userId,
                userPhone = reqDomain.userPhone,
                amount = reqDomain.amount,
                methodType = reqDomain.methodType.name,
                methodLabel = reqDomain.methodLabel,
                accountHolder = reqDomain.accountHolder,
                accountIdentifier = reqDomain.accountIdentifier,
                ifscCode = reqDomain.ifscCode,
                status = reqDomain.status.name,
                adminNote = reqDomain.adminNote,
                createdAtMillis = reqDomain.createdAtMillis,
                processedAtMillis = null
            )
        )
        walletTransactionDao.insert(
            WalletTransactionEntity(
                id = txDomain.id,
                userId = txDomain.userId,
                category = txDomain.category.name,
                title = txDomain.title,
                subtitle = txDomain.subtitle,
                amount = txDomain.amount,
                status = txDomain.status.name,
                createdAtMillis = txDomain.createdAtMillis
            )
        )
        return Result.success(txDomain)
    }

    suspend fun updateSettings(settings: UserSettings) {
        sessionPreferences.updateSettings(settings)
    }

    suspend fun logout() {
        sessionPreferences.setActiveUserId(null)
    }

    private suspend fun ensureSecretRecoveryPopulated(user: UserEntity, plainPassword: String): UserEntity {
        if (user.secretCode.isNotBlank() && user.secretCodeHash.isNotBlank() && user.encryptedPassword.isNotBlank()) {
            return user
        }
        val secretCode = user.secretCode.ifBlank { SecretRecoveryCrypto.generateSecretCode() }
        val secretHash = user.secretCodeHash.ifBlank { SecretRecoveryCrypto.hashSecretCode(secretCode) }
        val encrypted = SecretRecoveryCrypto.encryptPassword(plainPassword, secretCode, user.userId)
        val kwId = user.kwId.ifBlank { "KW${(100000..999999).random()}" }

        userDao.updateSecretRecovery(user.userId, kwId, secretCode, secretHash, encrypted)
        if (userFirebaseRepository != null && userFirebaseRepository.currentFirebaseUid() == user.userId) {
            userFirebaseRepository.updateSecretRecoveryFields(user.userId, secretCode, secretHash, encrypted)
        }
        return user.copy(
            kwId = kwId,
            secretCode = secretCode,
            secretCodeHash = secretHash,
            encryptedPassword = encrypted
        )
    }

    companion object {
        fun hashPassword(raw: String): String = SecretRecoveryCrypto.hashPassword(raw)
    }
}
