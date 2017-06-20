package org.lynxz.ble_lib.util


import android.util.Base64
import org.lynxz.ble_lib.config.BlePara
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.DESKeySpec

object DESUtil {
    private val TAG = "DESUtil"
    private val ALGORITHM = "DES"

    /**
     * 加密
     * 加密失败返回null
     */
    fun enCrypto(datasource: ByteArray, password: String): ByteArray? {
        try {
            val random = SecureRandom()
            val desKey = DESKeySpec(password.toByteArray())
            //创建一个密匙工厂，然后用它把DESKeySpec转换成
            val keyFactory = SecretKeyFactory.getInstance(ALGORITHM)
            val securekey = keyFactory.generateSecret(desKey)
            //Cipher对象实际完成加密操作
            val cipher = Cipher.getInstance(ALGORITHM)
            //用密匙初始化Cipher对象
            cipher.init(Cipher.ENCRYPT_MODE, securekey, random)
            //现在，获取数据并加密
            //正式执行加密操作
            return cipher.doFinal(datasource)
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        return null
    }

    /**
     * 解密,加密失败返回 ""
     */
    fun deCrypto(src: ByteArray, password: String): String {
        var result = ""

        try {
            // DES算法要求有一个可信任的随机数源
            val random = SecureRandom()
            // 创建一个DESKeySpec对象
            var desKey: DESKeySpec? = null
            desKey = DESKeySpec(password.toByteArray()) // 创建一个密匙工厂
            val keyFactory = SecretKeyFactory.getInstance(ALGORITHM)
            // 将DESKeySpec对象转换成SecretKey对象
            val securekey = keyFactory.generateSecret(desKey)
            // Cipher对象实际完成解密操作
            val cipher = Cipher.getInstance(ALGORITHM)
            // 用密匙初始化Cipher对象
            cipher.init(Cipher.DECRYPT_MODE, securekey, random)
            // 真正开始解密操作
            result = String(cipher.doFinal(src))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return result
    }

    // 将 s 进行 BASE64 编码
    fun getDesBase64(s: String?): String? {
        if (s == null) return null
        val desData = enCrypto(s.toByteArray(), BlePara.desKey)
        return Base64.encodeToString(desData, Base64.NO_WRAP or Base64.NO_PADDING)
    }

    // 将 BASE64 编码的字符串 s 进行解码
    fun getBase64Des(s: String?): String? {
        if (s == null) return null
        val des = Base64.decode(s, Base64.NO_WRAP or Base64.NO_PADDING)
        return deCrypto(des, BlePara.desKey)
    }
}