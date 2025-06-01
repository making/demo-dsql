package com.example.config;

import com.example.cart.Cart;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@RegisterReflectionForBinding({ Cart.class })
public class NativeConfig {

}
