package com.ing.f2etraining;

import static org.assertj.core.api.Assertions.assertThat;

import akka.dispatch.ExecutionContexts;
import akka.dispatch.Futures;
import akka.util.Timeout;
import com.ing.f2etraining.model.Person;
import errors.GenericError;
import errors.impl.MyError;
import java.util.concurrent.Executors;
import org.junit.Test;
import scala.concurrent.Await;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.util.Either;
import scala.util.Left;
import scala.util.Right;
import util.Java8;

public class CombineFuturesTest {

    private static final ExecutionContext EXECUTOR = ExecutionContexts.fromExecutor(Executors.newSingleThreadExecutor());
    private static final Timeout TIMEOUT = new Timeout(Duration.create(5, "seconds"));

    @Test
    public void combineSeveralFuturesWithFlatMap() throws Exception {
        //given
        Future<Person> meFuture = Futures.successful(new Person().setName("Juan").setAge(35));
        Future<Person> friendFuture = Futures.successful(new Person().setName("Miguel").setAge(28));

        //when
        Future<Integer> sumAgeF = meFuture.flatMap(
            me -> friendFuture.flatMap(
                friend -> Futures.successful(me.getAge() + friend.getAge())
                , EXECUTOR)
            , EXECUTOR);

        //then
        Integer sumAge = (Integer) Await.result(sumAgeF, TIMEOUT.duration());
        assertThat(sumAge).isEqualTo(63);
    }

    @Test
    public void combineSeveralFuturesWithMap() throws Exception {
        //given
        Future<Person> meFuture = Futures.successful(new Person().setName("Juan").setAge(35));
        Future<Person> friendFuture = Futures.successful(new Person().setName("Miguel").setAge(28));

        //when
        Future<Future<Integer>> sumAgeFF = meFuture.map(
            me -> friendFuture.map(
                friend -> friend.getAge() + me.getAge()
                , EXECUTOR)
            , EXECUTOR);

        //then
        Future<Integer> sumAgeF = Await.result(sumAgeFF, TIMEOUT.duration());
        Integer sumAge = (Integer) Await.result(sumAgeF, TIMEOUT.duration());
        assertThat(sumAge).isEqualTo(63);
    }

    @Test
    public void combineSeveralFuturesWithFlatMapAndMap() throws Exception {
        //given
        Future<Person> meFuture = Futures.successful(new Person().setName("Juan").setAge(35));
        Future<Person> friendFuture = Futures.successful(new Person().setName("Miguel").setAge(28));

        //when
        Future<Integer> sumAgeF = meFuture.flatMap(
            me ->friendFuture.map(
                friend -> friend.getAge() + me.getAge()
                , EXECUTOR)
            , EXECUTOR);

        //then
        Integer sumAge = (Integer) Await.result(sumAgeF, TIMEOUT.duration());
        assertThat(sumAge).isEqualTo(63);
    }

    @Test
    public void combineSeveralDependentFuturesWithFlatMapAndMap() throws Exception {
        //given
        Future<Person> meFuture = Futures.successful(new Person().setName("Juan").setAge(35));
        //Use service Future<Person> getFriend(String name)

        //when
        Future<Integer> sumAgeF = meFuture.flatMap(
            person -> getFriend(person.getName()).map(
                friend -> person.getAge() + friend.getAge()
                , EXECUTOR)
            , EXECUTOR);

        //then
        Integer sumAge = (Integer) Await.result(sumAgeF, TIMEOUT.duration());
        assertThat(sumAge).isEqualTo(63);
    }

    @Test
    public void combineSeveralFuturesWithFailureResult() throws Exception {
        //given
        Future<Person> meFuture = Futures.failed(new Exception("Unexpected error"));
        Future<Person> friendFuture = Futures.successful(new Person().setName("Miguel").setAge(28));

        //when
        /* TODO */
        Future<Integer> sumAgeF = meFuture.flatMap(
            me -> friendFuture.map(
                friend -> me.getAge() + friend.getAge()
                ,EXECUTOR
            )
            , EXECUTOR
        ).recover(Java8.recoverF(error -> -1), EXECUTOR);

        //then
        Integer sumAge = (Integer) Await.result(sumAgeF, TIMEOUT.duration());
        assertThat(sumAge).isEqualTo(-1);
    }

    @Test
    public void combineSeveralFuturesWithAnotherFailureResult() throws Exception {
        //given
        Future<Person> meFuture = Futures.successful(new Person().setName("Juan").setAge(35));
        Future<Person> friendFuture = Futures.failed(new Exception("Another unexpected error"));

        //when
        /* TODO */
        Future<Integer> sumAgeF = meFuture.flatMap(
            me -> friendFuture.map(
                friend -> me.getAge() + friend.getAge()
                ,EXECUTOR
            )
            , EXECUTOR
        ).recover(Java8.recoverF(error -> -1), EXECUTOR);

        //then
        Integer sumAge = (Integer) Await.result(sumAgeF, TIMEOUT.duration());
        assertThat(sumAge).isEqualTo(-1);
    }

    @Test
    public void combineSeveralFuturesWithALeftResult() throws Exception {
        //given
        Future<Either<GenericError, Person>> meFuture = Futures.successful(new Right<>(new Person().setName("Juan").setAge(35)));
        Future<Either<GenericError, Person>> friendFuture = Futures.successful(new Left<>(new MyError("Invalid user")));

        //when
        /* TODO */
        Future<Integer> sumAgeF = meFuture.flatMap(
            me -> friendFuture.map(
                friend -> {
                    if(me.isLeft() || friend.isLeft()) {
                        return -2;
                    }
                    return me.right().get().getAge() + friend.right().get().getAge();
                }
                ,EXECUTOR
            )
            , EXECUTOR
        ).recover(Java8.recoverF(error -> -1), EXECUTOR);

        //then
        Integer sumAge = (Integer) Await.result(sumAgeF, TIMEOUT.duration());
        assertThat(sumAge).isEqualTo(-2);
    }


    private Future<Person> getFriend(String name) {

        return Futures.successful(new Person().setName("Miguel").setAge(28));
    }
}
