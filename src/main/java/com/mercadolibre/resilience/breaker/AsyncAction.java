package com.mercadolibre.resilience.breaker;

import com.mercadolibre.common.async.Callback;


public interface AsyncAction<T,R> extends Verifiable<T> {

    R get(Callback<T> callback);

}
