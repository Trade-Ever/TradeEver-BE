package com.trever.backend.api.vehicle.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QVehicleOptionMapping is a Querydsl query type for VehicleOptionMapping
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QVehicleOptionMapping extends EntityPathBase<VehicleOptionMapping> {

    private static final long serialVersionUID = 2068625123L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QVehicleOptionMapping vehicleOptionMapping = new QVehicleOptionMapping("vehicleOptionMapping");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QVehicleOption option;

    public final QVehicle vehicle;

    public QVehicleOptionMapping(String variable) {
        this(VehicleOptionMapping.class, forVariable(variable), INITS);
    }

    public QVehicleOptionMapping(Path<? extends VehicleOptionMapping> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QVehicleOptionMapping(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QVehicleOptionMapping(PathMetadata metadata, PathInits inits) {
        this(VehicleOptionMapping.class, metadata, inits);
    }

    public QVehicleOptionMapping(Class<? extends VehicleOptionMapping> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.option = inits.isInitialized("option") ? new QVehicleOption(forProperty("option")) : null;
        this.vehicle = inits.isInitialized("vehicle") ? new QVehicle(forProperty("vehicle"), inits.get("vehicle")) : null;
    }

}

