package nl.tweeenveertig.csveed.bean;

import nl.tweeenveertig.csveed.line.Header;
import nl.tweeenveertig.csveed.line.RowReader;
import nl.tweeenveertig.csveed.api.Row;
import nl.tweeenveertig.csveed.line.RowReaderImpl;
import nl.tweeenveertig.csveed.report.CsvException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class BeanReaderImpl<T> implements BeanReader<T> {

    public static final Logger LOG = LoggerFactory.getLogger(BeanReaderImpl.class);

    private RowReader rowReader;

    private BeanReaderInstructionsImpl beanReaderInstructions;

    private AbstractMapper<T, Object> mapper;

    public BeanReaderImpl(Reader reader, Class<T> beanClass) {
        this(reader, new BeanParser().getBeanInstructions(beanClass));
    }

    public BeanReaderImpl(Reader reader, BeanReaderInstructions beanReaderInstructions) {
        this.beanReaderInstructions = (BeanReaderInstructionsImpl)beanReaderInstructions;
        this.rowReader = new RowReaderImpl(reader, this.beanReaderInstructions.getLineReaderInstructions());
    }

    public AbstractMapper<T, Object> getMapper() {
        if (this.mapper == null) {
            this.mapper = this.createMappingStrategy();
            mapper.setBeanReaderInstructions(this.beanReaderInstructions);
        }
        return mapper;
    }

    public List<T> readBeans() {
        List<T> beans = new ArrayList<T>();
        while (!isFinished()) {
            T bean = readBean();
            if (bean != null) {
                beans.add(bean);
            }
        }
        return beans;
    }

    public T readBean() {
        logSettings();
        Row unmappedRow = rowReader.readRow();
        if (unmappedRow == null) {
            return null;
        }
        getMapper().verifyHeader(unmappedRow);
        return getMapper().convert(instantiateBean(), unmappedRow, getCurrentLine());
    }

    protected void logSettings() {
        this.beanReaderInstructions.logSettings();
    }

    @Override
    public Header readHeader() {
        return rowReader.readHeader();
    }

    public int getCurrentLine() {
        return this.rowReader.getCurrentLine();
    }

    public boolean isFinished() {
        return rowReader.isFinished();
    }

    @Override
    public RowReader getRowReader() {
        return this.rowReader;
    }

    private T instantiateBean() {
        try {
            return this.getBeanClass().newInstance();
        } catch (Exception err) {
            String errorMessage =
                    "Unable to instantiate the bean class "+this.getBeanClass().getName()+
                    ". Does it have a no-arg public constructor?";
            LOG.error(errorMessage);
            throw new CsvException(errorMessage, err);
        }
    }

    @SuppressWarnings("unchecked")
    public Class<T> getBeanClass() {
        return this.beanReaderInstructions.getBeanClass();
    }

    @SuppressWarnings("unchecked")
    public AbstractMapper<T, Object> createMappingStrategy() {
        try {
            return this.beanReaderInstructions.getMappingStrategy().newInstance();
        } catch (Exception err) {
            throw new CsvException("Unable to instantiate the mapping strategy", err);
        }
    }

    public BeanReaderInstructions getBeanReaderInstructions() {
        return this.beanReaderInstructions;
    }

}
