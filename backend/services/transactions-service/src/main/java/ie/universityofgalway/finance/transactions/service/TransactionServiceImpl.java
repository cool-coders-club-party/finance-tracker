package ie.universityofgalway.finance.transactions.service;

import ie.universityofgalway.finance.transactions.entity.TransactionCategory;
import ie.universityofgalway.finance.transactions.entity.TransactionCurrency;
import ie.universityofgalway.finance.transactions.entity.TransactionType;
import ie.universityofgalway.finance.transactions.rest.dto.*;
import ie.universityofgalway.finance.transactions.entity.Transaction;
import ie.universityofgalway.finance.transactions.exception.InvalidTransactionHistoryQueryException;
import ie.universityofgalway.finance.transactions.exception.TransactionNotFoundException;
import ie.universityofgalway.finance.transactions.mapper.TransactionMapper;
import ie.universityofgalway.finance.transactions.repository.TransactionRepository;
import ie.universityofgalway.finance.transactions.repository.TransactionSpecifications;
import ie.universityofgalway.finance.transactions.util.validator.TransactionCategoryValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final TransactionCategoryValidator transactionCategoryValidator;

    @Override
    @Transactional
    public TransactionResponseDto createTransaction(CreateTransactionRequest request, UUID userId) {
        transactionCategoryValidator.validate(request.transactionType(), request.transactionCategory());
        Transaction transaction = transactionMapper.to(request);
        transaction.setUserId(userId);
        return transactionMapper.from(transactionRepository.save(transaction));
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionPageResponseDto findHistory(TransactionHistoryQuery query) {
        PageRequest pageable = createPageRequest(query);
        return transactionMapper.from(transactionRepository.findAll(
                TransactionSpecifications.forHistory(
                        query.userId(), query.type(), query.category(), query.from(), query.to()), pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponseDto findTransactionById(Long transactionId, UUID userId) {
        return transactionMapper.from(transactionRepository.findActiveByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId)));
    }

    @Override
    @Transactional
    public TransactionResponseDto updateTransaction(Long transactionId, UpdateTransactionRequest request, UUID userId) {
        Transaction transaction = transactionRepository.findActiveByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));

        TransactionType transactionType = request.transactionType() == null ?
                transaction.getTransactionType() : request.transactionType();
        TransactionCategory transactionCategory = request.transactionCategory() == null ?
                transaction.getTransactionCategory() : request.transactionCategory();

        transactionCategoryValidator.validate(transactionType, transactionCategory);
        transactionMapper.update(transaction, request);
        return transactionMapper.from(transaction);
    }

    @Override
    @Transactional
    public void softDelete(Long transactionId, UUID userId) {
        Integer deletedTransactions = transactionRepository.softDelete(transactionId, userId);
        if (deletedTransactions == 0) throw new TransactionNotFoundException(transactionId);
    }

    @Override
    @Transactional
    public void hardDelete(Long transactionId, UUID userId) {
        Integer deletedTransactions = transactionRepository.hardDelete(transactionId, userId);
        if (deletedTransactions == 0) throw new TransactionNotFoundException(transactionId);
    }

    @Override
    public TransactionMetadataResponseDto findTransactionMetadata() {
        TransactionType[] types = TransactionType.values();
        TransactionCategory[] categories = TransactionCategory.values();
        List<TransactionCategoryMetadataDto> categoriesMetadataDto = Arrays.stream(categories)
                .map(category -> new TransactionCategoryMetadataDto(
                        category,
                        Arrays.stream(types)
                                .filter(category::allows)
                                .toList()
                ))
                .toList();
        return new TransactionMetadataResponseDto(
                Arrays.asList(types),
                categoriesMetadataDto,
                Arrays.asList(TransactionCurrency.values())
        );
    }

    @Override
    @Transactional
    public TransactionResponseDto restoreAndGetSoftDeletedTransaction(Long transactionId, UUID userId) {
        Transaction transaction = transactionRepository.findByIdAndUserIdAndActiveFalse(
                transactionId,
                userId
        ).orElseThrow(() -> new TransactionNotFoundException(transactionId));

        transaction.setActive(true);
        transactionRepository.save(transaction);
        return transactionMapper.from(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionPageResponseDto findSoftDeletedTransactions(TransactionHistoryQuery query) {
        PageRequest pageable = createPageRequest(query);

        return transactionMapper.from(transactionRepository.findAll(
                TransactionSpecifications.forSoftDeleted(
                        query.userId(), query.type(), query.category(), query.from(), query.to()), pageable));
    }

    private PageRequest createPageRequest(TransactionHistoryQuery query) {
        if (query.from() != null && query.to() != null && query.from().isAfter(query.to())) {
            throw new InvalidTransactionHistoryQueryException("from must be on or before to");
        }

        String sortProperty = switch (query.sortBy().toLowerCase(Locale.ROOT)) {
            case "date", "occurredat" -> "occurredAt";
            case "amount" -> "amount";
            default -> throw new InvalidTransactionHistoryQueryException("sortBy must be date or amount");
        };
        Sort.Direction direction = switch (query.direction().toLowerCase(Locale.ROOT)) {
            case "asc" -> Sort.Direction.ASC;
            case "desc" -> Sort.Direction.DESC;
            default -> throw new InvalidTransactionHistoryQueryException("direction must be asc or desc");
        };

        Sort sort = Sort.by(
                new Sort.Order(direction, sortProperty),
                new Sort.Order(Sort.Direction.DESC, "id")
        );
        return PageRequest.of(query.page(), query.size(), sort);
    }


}
