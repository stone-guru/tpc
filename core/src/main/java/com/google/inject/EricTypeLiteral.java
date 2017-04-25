package com.google.inject;

import com.google.inject.util.Types;

public class EricTypeLiteral<A, B> extends TypeLiteral<A> {
    public EricTypeLiteral(Class<A> ca, Class<B> cb) {
        super(Types.newParameterizedTypeWithOwner(ca.getEnclosingClass(), ca, cb));
    }
}
