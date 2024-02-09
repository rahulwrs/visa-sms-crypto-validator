package com.zimblesystems.cryptoValidator.exception;

public class HSMKeyNotFoundException extends Exception{

        public HSMKeyNotFoundException(String errorMessage){

            super(errorMessage);
//            String message = new StringBuilder()
//                    .append(" No Key Found for Org : ")
//                    .append(org)
//                    .append(" product  : ")
//                    .append(product)
//                    .append(" For Key Type : ")
//                    .append(keyType)
//                    .toString();


        }

}
