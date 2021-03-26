package org.acme;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.function.Supplier;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import io.micrometer.core.instrument.MeterRegistry;

@Path("/book")
public class BookResource {

    private static Map<Long, String> books = new HashMap<>();

    private final MeterRegistry registry;
    private final LongAccumulator highestRating = new LongAccumulator(Long::max, 0);
    
    public BookResource(MeterRegistry registry) {

        this.registry = registry;
        registry.gauge("book.rating.max", this,
                BookResource::highestRatingBook);

        populateBooks();
    }

    long highestRatingBook() {
        return highestRating.get();
    }

    private void populateBooks() {
        books.put(1L, "Book 1");
        books.put(2L, "Book 2");
        books.put(3L, "Book 3");
        books.put(4L, "Book 4");
        books.put(5L, "Book 5");
    }

    @RestClient
    RatingService ratingService;

    private static final Logger LOG = Logger.getLogger(BookResource.class);

    @GET
    @Path("/{bookId}")
    @RolesAllowed("Echoer")
    @Produces(MediaType.APPLICATION_JSON)
    public Book book(@PathParam("bookId") Long bookId) {
        LOG.info("Get Book");

        final Book bookObject = new Book();
        bookObject.bookId = bookId;
        bookObject.name = findBook(bookId);
        Supplier<Rate> rateSupplier = () -> {
            return ratingService.getRate(bookId);
        };
        
        final Rate rate = registry.timer("book.rating.test").wrap(rateSupplier).get();

        bookObject.rating = rate.rate;

        highestRating.accumulate(rate.rate);

        return bookObject;
    }

    private String findBook(Long bookId) {
        return books.get(bookId);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getBook(Book book) {
        return Response.created(
                    UriBuilder.fromResource(BookResource.class)
                      .path(Long.toString(book.bookId))
                      .build())
                .build();
    }

    @DELETE
    @Path("/{bookId}")
    public Response delete(@PathParam("bookId") Long bookId) {
        return Response.noContent().build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("search")
    public Response searchBook(@QueryParam("description") String description) {
        List<Book> books = new ArrayList<>();
        return Response.ok(books).build();
    }
}