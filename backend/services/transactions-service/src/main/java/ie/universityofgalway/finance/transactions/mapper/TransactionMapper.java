package ie.universityofgalway.finance.transactions.mapper;

import ie.universityofgalway.finance.transactions.rest.dto.CreateTransactionRequest;
import ie.universityofgalway.finance.transactions.rest.dto.TransactionPageResponseDto;
import ie.universityofgalway.finance.transactions.rest.dto.TransactionResponseDto;
import ie.universityofgalway.finance.transactions.rest.dto.UpdateTransactionRequest;
import ie.universityofgalway.finance.transactions.entity.Transaction;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

import java.util.List;

import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;
import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(
        componentModel = SPRING,
        injectionStrategy = CONSTRUCTOR,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public abstract class TransactionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    public abstract Transaction to(CreateTransactionRequest request);

    public abstract List<Transaction> to(List<CreateTransactionRequest> requests);

    public abstract TransactionResponseDto from(Transaction transaction);

    public abstract List<TransactionResponseDto> from(List<Transaction> transactions);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    public abstract void update(@MappingTarget Transaction transaction, UpdateTransactionRequest request);

    public TransactionPageResponseDto from(Page<Transaction> transactions) {
        return new TransactionPageResponseDto(
                from(transactions.getContent()),
                transactions.getNumber(),
                transactions.getSize(),
                transactions.getTotalElements(),
                transactions.getTotalPages());
    }
}
