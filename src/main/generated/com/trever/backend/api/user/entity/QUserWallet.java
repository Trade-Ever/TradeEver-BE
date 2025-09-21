package com.trever.backend.api.user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUserWallet is a Querydsl query type for UserWallet
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserWallet extends EntityPathBase<UserWallet> {

    private static final long serialVersionUID = -440729697L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QUserWallet userWallet = new QUserWallet("userWallet");

    public final com.trever.backend.common.entity.QBaseTimeEntity _super = new com.trever.backend.common.entity.QBaseTimeEntity(this);

    public final NumberPath<Long> balance = createNumber("balance", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final QUser user;

    public QUserWallet(String variable) {
        this(UserWallet.class, forVariable(variable), INITS);
    }

    public QUserWallet(Path<? extends UserWallet> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QUserWallet(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QUserWallet(PathMetadata metadata, PathInits inits) {
        this(UserWallet.class, metadata, inits);
    }

    public QUserWallet(Class<? extends UserWallet> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new QUser(forProperty("user")) : null;
    }

}

