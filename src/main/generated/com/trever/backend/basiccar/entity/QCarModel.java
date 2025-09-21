package com.trever.backend.basiccar.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QCarModel is a Querydsl query type for CarModel
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCarModel extends EntityPathBase<CarModel> {

    private static final long serialVersionUID = -869603785L;

    public static final QCarModel carModel = new QCarModel("carModel");

    public final StringPath carName = createString("carName");

    public final NumberPath<Integer> carYear = createNumber("carYear", Integer.class);

    public final StringPath category = createString("category");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath manufacturer = createString("manufacturer");

    public final StringPath modelName = createString("modelName");

    public QCarModel(String variable) {
        super(CarModel.class, forVariable(variable));
    }

    public QCarModel(Path<? extends CarModel> path) {
        super(path.getType(), path.getMetadata());
    }

    public QCarModel(PathMetadata metadata) {
        super(CarModel.class, metadata);
    }

}

