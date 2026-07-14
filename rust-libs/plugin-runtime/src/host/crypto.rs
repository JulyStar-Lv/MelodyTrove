use crate::PluginRuntimeError;
use aes::{Aes128, Aes192, Aes256};
use base64::{engine::general_purpose, Engine as _};
use cipher::{block_padding::Pkcs7, BlockDecryptMut, BlockEncryptMut, KeyInit};
use md5::{Digest, Md5};

pub fn md5_hex(data: &[u8]) -> String {
    let mut digest = Md5::new();
    digest.update(data);
    digest
        .finalize()
        .iter()
        .map(|value| format!("{value:02x}"))
        .collect()
}

pub fn aes_ecb_encrypt_base64(text: &str, key: &str) -> Result<String, PluginRuntimeError> {
    Ok(general_purpose::STANDARD.encode(aes_encrypt(text.as_bytes(), key.as_bytes())?))
}

pub fn aes_ecb_encrypt_hex(text: &str, key: &str) -> Result<String, PluginRuntimeError> {
    Ok(aes_encrypt(text.as_bytes(), key.as_bytes())?
        .iter()
        .map(|value| format!("{value:02x}"))
        .collect())
}

pub fn aes_ecb_decrypt_base64(encoded: &str, key: &str) -> Result<String, PluginRuntimeError> {
    let encrypted = general_purpose::STANDARD
        .decode(encoded)
        .or_else(|_| general_purpose::STANDARD_NO_PAD.decode(encoded))
        .map_err(host_error)?;
    let decrypted = aes_decrypt(&encrypted, key.as_bytes())?;
    String::from_utf8(decrypted).map_err(host_error)
}

fn aes_encrypt(data: &[u8], key: &[u8]) -> Result<Vec<u8>, PluginRuntimeError> {
    let mut buffer = vec![0; data.len() + 16];
    buffer[..data.len()].copy_from_slice(data);
    macro_rules! encrypt {
        ($cipher:ty) => {
            <$cipher>::new_from_slice(key)
                .map_err(host_error)?
                .encrypt_padded_mut::<Pkcs7>(&mut buffer, data.len())
                .map_err(host_error)?
                .to_vec()
        };
    }
    match key.len() {
        16 => Ok(encrypt!(Aes128)),
        24 => Ok(encrypt!(Aes192)),
        32 => Ok(encrypt!(Aes256)),
        _ => Err(host_error("AES key must be 16, 24, or 32 bytes")),
    }
}

fn aes_decrypt(data: &[u8], key: &[u8]) -> Result<Vec<u8>, PluginRuntimeError> {
    let mut buffer = data.to_vec();
    macro_rules! decrypt {
        ($cipher:ty) => {
            <$cipher>::new_from_slice(key)
                .map_err(host_error)?
                .decrypt_padded_mut::<Pkcs7>(&mut buffer)
                .map_err(host_error)?
                .to_vec()
        };
    }
    match key.len() {
        16 => Ok(decrypt!(Aes128)),
        24 => Ok(decrypt!(Aes192)),
        32 => Ok(decrypt!(Aes256)),
        _ => Err(host_error("AES key must be 16, 24, or 32 bytes")),
    }
}

fn host_error(error: impl std::fmt::Display) -> PluginRuntimeError {
    PluginRuntimeError::HostApi(error.to_string())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn crypto_golden_values() {
        assert_eq!(md5_hex(b"abc"), "900150983cd24fb0d6963f7d28e17f72");
        let encrypted = aes_ecb_encrypt_base64("hello", "1234567890123456").unwrap();
        assert_eq!(
            aes_ecb_decrypt_base64(&encrypted, "1234567890123456").unwrap(),
            "hello"
        );
        assert!(!aes_ecb_encrypt_hex("hello", "1234567890123456")
            .unwrap()
            .is_empty());
    }
}
