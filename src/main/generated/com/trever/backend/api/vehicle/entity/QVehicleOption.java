package com.trever.backend.api.vehicle.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QVehicleOption is a Querydsl query type for VehicleOption
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QVehicleOption extends EntityPathBase<VehicleOption> {

    private static final long serialVersionUID = 1808358251L;

    public static final QVehicleOption vehicleOption = new QVehicleOption("vehicleOption");

    public final com.trever.backend.common.entity.QBaseTimeEntity _super = new com.trever.backend.common.entity.QBaseTimeEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath description = createString("description");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath name = createString("name");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QVehicleOption(String variable) {
        super(VehicleOption.class, forVariable(variable));
    }

    public QVehicleOption(Path<? extends VehicleOption> path) {
        super(path.getType(), path.getMetadata());
    }

    public QVehicleOption(PathMetadata metadata) {
        super(VehicleOption.class, metadata);
    }

}

