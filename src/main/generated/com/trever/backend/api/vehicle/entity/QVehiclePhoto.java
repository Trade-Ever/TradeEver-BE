package com.trever.backend.api.vehicle.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QVehiclePhoto is a Querydsl query type for VehiclePhoto
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QVehiclePhoto extends EntityPathBase<VehiclePhoto> {

    private static final long serialVersionUID = 1583035516L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QVehiclePhoto vehiclePhoto = new QVehiclePhoto("vehiclePhoto");

    public final com.trever.backend.common.entity.QBaseTimeEntity _super = new com.trever.backend.common.entity.QBaseTimeEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> orderIndex = createNumber("orderIndex", Integer.class);

    public final StringPath photoUrl = createString("photoUrl");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final QVehicle vehicle;

    public QVehiclePhoto(String variable) {
        this(VehiclePhoto.class, forVariable(variable), INITS);
    }

    public QVehiclePhoto(Path<? extends VehiclePhoto> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QVehiclePhoto(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QVehiclePhoto(PathMetadata metadata, PathInits inits) {
        this(VehiclePhoto.class, metadata, inits);
    }

    public QVehiclePhoto(Class<? extends VehiclePhoto> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.vehicle = inits.isInitialized("vehicle") ? new QVehicle(forProperty("vehicle"), inits.get("vehicle")) : null;
    }

}

