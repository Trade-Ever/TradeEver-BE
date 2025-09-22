package com.trever.backend.api.vehicle.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QVehicle is a Querydsl query type for Vehicle
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QVehicle extends EntityPathBase<Vehicle> {

    private static final long serialVersionUID = -1234354346L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QVehicle vehicle = new QVehicle("vehicle");

    public final com.trever.backend.common.entity.QBaseTimeEntity _super = new com.trever.backend.common.entity.QBaseTimeEntity(this);

    public final StringPath accidentDescription = createString("accidentDescription");

    public final ComparablePath<Character> accidentHistory = createComparable("accidentHistory", Character.class);

    public final NumberPath<Long> auctionId = createNumber("auctionId", Long.class);

    public final StringPath carName = createString("carName");

    public final StringPath carNumber = createString("carNumber");

    public final StringPath color = createString("color");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath description = createString("description");

    public final NumberPath<Integer> engineCc = createNumber("engineCc", Integer.class);

    public final NumberPath<Integer> favoriteCount = createNumber("favoriteCount", Integer.class);

    public final StringPath fuelType = createString("fuelType");

    public final NumberPath<Integer> horsepower = createNumber("horsepower", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final ComparablePath<Character> isAuction = createComparable("isAuction", Character.class);

    public final StringPath manufacturer = createString("manufacturer");

    public final NumberPath<Integer> mileage = createNumber("mileage", Integer.class);

    public final StringPath model = createString("model");

    public final ListPath<VehicleOptionMapping, QVehicleOptionMapping> optionMappings = this.<VehicleOptionMapping, QVehicleOptionMapping>createList("optionMappings", VehicleOptionMapping.class, QVehicleOptionMapping.class, PathInits.DIRECT2);

    public final ListPath<VehiclePhoto, QVehiclePhoto> photos = this.<VehiclePhoto, QVehiclePhoto>createList("photos", VehiclePhoto.class, QVehiclePhoto.class, PathInits.DIRECT2);

    public final NumberPath<Long> price = createNumber("price", Long.class);

    public final StringPath representativePhotoUrl = createString("representativePhotoUrl");

    public final com.trever.backend.api.user.entity.QUser seller;

    public final StringPath transmission = createString("transmission");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final EnumPath<VehicleStatus> vehicleStatus = createEnum("vehicleStatus", VehicleStatus.class);

    public final EnumPath<VehicleType> vehicleType = createEnum("vehicleType", VehicleType.class);

    public final NumberPath<Integer> year_value = createNumber("year_value", Integer.class);

    public QVehicle(String variable) {
        this(Vehicle.class, forVariable(variable), INITS);
    }

    public QVehicle(Path<? extends Vehicle> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QVehicle(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QVehicle(PathMetadata metadata, PathInits inits) {
        this(Vehicle.class, metadata, inits);
    }

    public QVehicle(Class<? extends Vehicle> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.seller = inits.isInitialized("seller") ? new com.trever.backend.api.user.entity.QUser(forProperty("seller")) : null;
    }

}

