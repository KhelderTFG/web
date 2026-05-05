interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label:    string;
  error?:   string;
}

const Input = ({ label, error, className = '', id, ...props }: InputProps) => {
  const inputId = id ?? label.toLowerCase().replace(/\s/g, '-');

  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={inputId} className="text-sm font-medium text-gray-700">
        {label}
      </label>
      <input
        id={inputId}
        className={`
          px-3 py-2 border rounded-lg outline-none transition-all
          focus:ring-2 focus:ring-[#1A8C7A] focus:border-[#1A8C7A]
          ${error ? 'border-red-500' : 'border-gray-300'}
          ${className}
        `}
        {...props}
      />
      {error && <p className="text-xs text-red-500">{error}</p>}
    </div>
  );
};

export default Input;