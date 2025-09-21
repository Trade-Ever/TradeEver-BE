package com.trever.backend.api.trade.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QTransaction is a Querydsl query type for Transaction
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTransaction extends EntityPathBase<Transaction> {

    private static final long serialVersionUID = -1159710320L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QTransaction transaction = new QTransaction("transaction");

    public final com.trever.backend.common.entity.QBaseTimeEntity _super = new com.trever.backend.common.entity.QBaseTimeEntity(this);

    public final com.trever.backend.api.user.entity.QUser buyer;

    public final DateTimePath<java.time.LocalDateTime> completedAt = createDateTime("completedAt", java.time.LocalDateTime.class);

    public final QContract contract;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> finalPrice = createNumber("finalPrice", Long.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final com.trever.backend.api.user.entity.QUser seller;

    public final EnumPath<TransactionStatus> status = createEnum("status", TransactionStatus.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final com.trever.backend.api.vehicle.entity.QVehicle vehicle;

    public QTransaction(String variable) {
        this(Transaction.class, forVariable(variable), INITS);
    }

    public QTransaction(Path<? extends Transaction> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QTransaction(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QTransaction(PathMetadata metadata, PathInits inits) {
        this(Transaction.class, metadata, inits);
    }

    public QTransaction(Class<? extends Transaction> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.buyer = inits.isInitialized("buyer") ? new com.trever.backend.api.user.entity.QUser(forProperty("buyer")) : null;
        this.contract = inits.isInitialized("contract") ? new QContract(forProperty("contract"), inits.get("contract")) : null;
        this.seller = inits.isInitialized("seller") ? new com.trever.backend.api.user.entity.QUser(forProperty("seller")) : null;
        this.vehicle = inits.isInitialized("vehicle") ? new com.trever.backend.api.vehicle.entity.QVehicle(forProperty("vehicle"), inits.get("vehicle")) : null;
    }

}

