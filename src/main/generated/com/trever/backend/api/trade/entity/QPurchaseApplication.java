package com.trever.backend.api.trade.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPurchaseApplication is a Querydsl query type for PurchaseApplication
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPurchaseApplication extends EntityPathBase<PurchaseApplication> {

    private static final long serialVersionUID = -461389247L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPurchaseApplication purchaseApplication = new QPurchaseApplication("purchaseApplication");

    public final com.trever.backend.common.entity.QBaseTimeEntity _super = new com.trever.backend.common.entity.QBaseTimeEntity(this);

    public final com.trever.backend.api.user.entity.QUser buyer;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final com.trever.backend.api.vehicle.entity.QVehicle vehicle;

    public QPurchaseApplication(String variable) {
        this(PurchaseApplication.class, forVariable(variable), INITS);
    }

    public QPurchaseApplication(Path<? extends PurchaseApplication> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPurchaseApplication(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPurchaseApplication(PathMetadata metadata, PathInits inits) {
        this(PurchaseApplication.class, metadata, inits);
    }

    public QPurchaseApplication(Class<? extends PurchaseApplication> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.buyer = inits.isInitialized("buyer") ? new com.trever.backend.api.user.entity.QUser(forProperty("buyer")) : null;
        this.vehicle = inits.isInitialized("vehicle") ? new com.trever.backend.api.vehicle.entity.QVehicle(forProperty("vehicle"), inits.get("vehicle")) : null;
    }

}

